package com.logicaldoc.core.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.conversion.FormatConverterManager;
import com.logicaldoc.core.folder.Folder;
import com.logicaldoc.core.folder.FolderDAO;
import com.logicaldoc.core.folder.FolderEvent;
import com.logicaldoc.core.folder.FolderHistory;
import com.logicaldoc.core.history.History;
import com.logicaldoc.core.metadata.Attribute;
import com.logicaldoc.core.metadata.Template;
import com.logicaldoc.core.metadata.TemplateDAO;
import com.logicaldoc.core.parser.ParseParameters;
import com.logicaldoc.core.parser.Parser;
import com.logicaldoc.core.parser.ParserFactory;
import com.logicaldoc.core.parser.ParsingException;
import com.logicaldoc.core.searchengine.SearchEngine;
import com.logicaldoc.core.security.Permission;
import com.logicaldoc.core.security.Session;
import com.logicaldoc.core.security.SessionManager;
import com.logicaldoc.core.security.TenantDAO;
import com.logicaldoc.core.security.authorization.PermissionException;
import com.logicaldoc.core.security.menu.Menu;
import com.logicaldoc.core.security.menu.MenuDAO;
import com.logicaldoc.core.security.user.Group;
import com.logicaldoc.core.security.user.User;
import com.logicaldoc.core.security.user.UserDAO;
import com.logicaldoc.core.store.Store;
import com.logicaldoc.core.threading.ThreadPools;
import com.logicaldoc.core.ticket.Ticket;
import com.logicaldoc.core.ticket.TicketDAO;
import com.logicaldoc.core.util.MergeUtil;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.config.ContextProperties;
import com.logicaldoc.util.html.HTMLSanitizer;
import com.logicaldoc.util.io.FileUtil;
import com.logicaldoc.util.time.TimeDiff;
import com.logicaldoc.util.time.TimeDiff.TimeField;

/**
 * Basic Implementation of <code>DocumentManager</code>
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 3.5
 */
@Component("documentManager")
public class DocumentManagerImpl implements DocumentManager {

	private static final String UPDATE_LD_DOCUMENT_SET_LD_INDEXED = "update ld_document set ld_indexed=";

	private static final String NO_VALUE_OBJECT_HAS_BEEN_PROVIDED = "No value object has been provided";

	private static final String TRANSACTION_CANNOT_BE_NULL = "transaction cannot be null";

	private static final String MERGE = "merge";

	private static final String UNKNOWN = "unknown";

	private static final String DOCUMENT_IS_IMMUTABLE = "Document is immutable";

	protected static Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

	private DocumentDAO documentDAO;

	private DocumentLinkDAO documentLinkDAO;

	private DocumentNoteDAO documentNoteDAO;

	private FolderDAO folderDAO;

	private TemplateDAO templateDAO;

	private DocumentListenerManager listenerManager;

	private VersionDAO versionDAO;

	private UserDAO userDAO;

	private TicketDAO ticketDAO;

	private SearchEngine indexer;

	private Store store;

	private ContextProperties config;

	@Autowired
	public DocumentManagerImpl(DocumentDAO documentDAO, DocumentLinkDAO documentLinkDAO,
			DocumentNoteDAO documentNoteDAO, FolderDAO folderDAO, TemplateDAO templateDAO,
			DocumentListenerManager listenerManager, VersionDAO versionDAO, UserDAO userDAO, TicketDAO ticketDAO,
			SearchEngine indexer, Store store, ContextProperties config) {
		super();
		this.documentDAO = documentDAO;
		this.documentLinkDAO = documentLinkDAO;
		this.documentNoteDAO = documentNoteDAO;
		this.folderDAO = folderDAO;
		this.templateDAO = templateDAO;
		this.listenerManager = listenerManager;
		this.versionDAO = versionDAO;
		this.userDAO = userDAO;
		this.ticketDAO = ticketDAO;
		this.indexer = indexer;
		this.store = store;
		this.config = config;
	}

	@Override
	public void replaceFile(long docId, String fileVersion, InputStream content, DocumentHistory transaction)
			throws IOException, PersistenceException {
		if (transaction == null)
			throw new IllegalArgumentException(TRANSACTION_CANNOT_BE_NULL);

		// Write content to temporary file, then delete it
		File tmp = FileUtil.createTempFile("replacefile", "");
		try {
			if (content != null)
				FileUtil.writeFile(content, tmp.getPath());
			replaceFile(docId, fileVersion, tmp, transaction);
		} finally {
			FileUtils.deleteQuietly(tmp);
		}
	}

	@Override
	public void replaceFile(long docId, String fileVersion, File newFile, DocumentHistory transaction)
			throws PersistenceException, IOException {
		validateTransaction(transaction);

		transaction.setEvent(DocumentEvent.VERSION_REPLACED.toString());
		transaction.setComment(String.format("file version %s - %s", fileVersion, transaction.getComment()));

		// identify the document and folder
		Document document = documentDAO.findDocument(docId);

		if (document.getImmutable() == 0 && document.getStatus() == AbstractDocument.DOC_UNLOCKED) {
			// Remove the ancillary files of the same fileVersion
			final String newFilerResourceName = store.getResourceName(document, fileVersion, null);
			for (String resource : store.listResources(document.getId(), fileVersion).stream()
					.filter(r -> !r.equals(newFilerResourceName)).toList())
				store.delete(document.getId(), resource);

			// Store the new file
			store.store(newFile, document.getId(), newFilerResourceName);

			long fileSize = newFile.length();

			// Now update the file size in the versions
			List<Version> versions = versionDAO.findByDocId(document.getId());
			for (Version version : versions) {
				if (version.getFileVersion().equals(fileVersion)) {
					versionDAO.initialize(version);
					version.setFileSize(fileSize);
					storeVersionAsync(version);
				}
			}

			// Update the document's gridRecord
			documentDAO.initialize(document);
			document.setFileSize(fileSize);
			if (document.getIndexed() != AbstractDocument.INDEX_SKIP)
				document.setIndexed(AbstractDocument.INDEX_TO_INDEX);
			document.setOcrd(0);
			document.setBarcoded(0);
			document.setSigned(0);
			document.setStamped(0);
			documentDAO.store(document, transaction);

			log.debug("Replaced fileVersion {} of document {}", fileVersion, docId);
		}
	}

	private void validateTransaction(History transaction) {
		if (transaction == null)
			throw new IllegalArgumentException(TRANSACTION_CANNOT_BE_NULL);
		if (transaction.getUser() == null)
			throw new IllegalArgumentException("transaction user cannot be null");
	}

	@Override
	public void checkin(long docId, File file, String filename, boolean release, AbstractDocument docVO,
			DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		if (filename == null)
			throw new IllegalArgumentException("File name is mandatory");

		transaction.setEvent(DocumentEvent.CHECKEDIN.toString());
		transaction.setFile(file.getAbsolutePath());

		/*
		 * Better to synchronize this block because under high multi-threading
		 * may lead to hibernate's sessions rollbacks
		 */
		synchronized (this) {
			// identify the document and folder
			Document document = documentDAO.findDocument(docId);
			String oldFileVersion = document.getFileVersion();

			document.setComment(transaction.getComment());

			Document oldDocument = null;
			if (document.getImmutable() != 0)
				return;

			documentDAO.initialize(document);

			oldDocument = new Document(document);

			// Check CustomId uniqueness
			checkCustomIdUniquenessOnCheckin(document, docVO);

			/*
			 * Now apply the metadata, if any
			 */
			if (docVO != null) {
				Folder originalFolder = document.getFolder();
				String originalVersion = document.getVersion();
				String originalFileVersion = document.getFileVersion();
				String originalFileName = document.getFileName();

				document.copyAttributes(docVO);

				// Restore important original information
				document.setFolder(originalFolder);
				document.setVersion(originalVersion);
				document.setFileVersion(originalFileVersion);
				if (StringUtils.isNotEmpty(filename))
					document.setFileName(filename);
				else
					document.setFileName(originalFileName);
			}

			countPages(file, document);

			Map<String, Object> dictionary = new HashMap<>();

			log.debug("Invoke listeners before checkin");
			for (DocumentListener listener : listenerManager.getListeners())
				listener.beforeCheckin(document, transaction, dictionary);

			document.setStamped(0);
			document.setSigned(0);
			document.setOcrd(0);
			document.setBarcoded(0);

			if (document.getIndexed() != AbstractDocument.INDEX_SKIP)
				document.setIndexed(AbstractDocument.INDEX_TO_INDEX);

			documentDAO.store(document);

			document = documentDAO.findById(document.getId());
			Folder folder = document.getFolder();
			documentDAO.initialize(document);

			// create some strings containing paths
			document.setFileName(filename);
			document.setType(FileUtil.getExtension(filename));

			// set other properties of the document
			document.setDate(new Date());
			document.setPublisher(transaction.getUsername());
			document.setPublisherId(transaction.getUserId());
			document.setStatus(AbstractDocument.DOC_UNLOCKED);
			document.setLockUserId(null);
			document.setFolder(folder);
			document.setDigest(null);
			document.setFileSize(file.length());
			document.setExtResId(null);

			// Create new version (a new version number is created)
			Version version = Version.create(document, transaction.getUser(), transaction.getComment(),
					DocumentEvent.CHECKEDIN.toString(), release);

			document.setStatus(AbstractDocument.DOC_UNLOCKED);
			documentDAO.store(document, transaction);

			// store the document in the repository (on the file system)
			try {
				storeFile(document, file);
			} catch (IOException ioe) {
				document.copyAttributes(oldDocument);
				document.setOcrd(oldDocument.getOcrd());
				document.setOcrTemplateId(oldDocument.getOcrTemplateId());
				document.setBarcoded(oldDocument.getBarcoded());
				document.setBarcodeTemplateId(oldDocument.getBarcodeTemplateId());
				document.setIndexed(oldDocument.getIndexed());
				document.setCustomId(oldDocument.getCustomId());
				document.setStatus(oldDocument.getStatus());
				document.setStamped(oldDocument.getStamped());
				document.setSigned(oldDocument.getSigned());
				document.setComment(oldDocument.getComment());
				documentDAO.store(document);
				throw new PersistenceException(String.format("Cannot save the new version %s into the store", document),
						ioe);
			}

			version.setFileSize(document.getFileSize());
			version.setDigest(null);
			storeVersionAsync(version);

			log.debug("Stored version {}", version.getVersion());
			log.debug("Invoke listeners after checkin");
			for (DocumentListener listener : listenerManager.getListeners())
				listener.afterCheckin(document, transaction, dictionary);
			documentDAO.store(document);

			log.debug("Checked in document {}", docId);

			if (!document.getFileVersion().equals(oldFileVersion))
				documentNoteDAO.copyAnnotations(document.getId(), oldFileVersion, document.getFileVersion());
		}
	}

	private void checkCustomIdUniquenessOnCheckin(Document document, AbstractDocument docVO)
			throws PersistenceException {
		if (docVO != null && docVO.getCustomId() != null) {
			Document test = documentDAO.findByCustomId(docVO.getCustomId(), document.getTenantId());
			if (test != null && test.getId() != document.getId())
				throw new PersistenceException("Duplicated CustomID");
		}
	}

	@Override
	public void checkin(long docId, InputStream content, String filename, boolean release, AbstractDocument docVO,
			DocumentHistory transaction) throws IOException, PersistenceException {
		validateTransaction(transaction);

		// Write content to temporary file, then delete it
		File tmp = FileUtil.createTempFile("checkin", "." + FileUtil.getExtension(filename));
		try {
			FileUtil.writeFile(content, tmp.getPath());
			checkin(docId, tmp, filename, release, docVO, transaction);
		} finally {
			FileUtils.deleteQuietly(tmp);
		}
	}

	@Override
	public void checkout(long docId, DocumentHistory transaction) throws PersistenceException {
		if (transaction.getEvent() == null)
			transaction.setEvent(DocumentEvent.CHECKEDOUT.toString());
		lock(docId, AbstractDocument.DOC_CHECKED_OUT, transaction);
	}

	@Override
	public void lock(long docId, int status, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		/*
		 * Better to synchronize this block because under high multi-threading
		 * may lead to hibernate's sessions rollbacks
		 */
		synchronized (this) {
			Document document = documentDAO.findDocument(docId);

			if (document.getStatus() == status && document.getLockUserId().equals(transaction.getUserId())) {
				log.debug("Document {} is already locked by user {}", document, transaction.getUser().getFullName());
				return;
			}

			if (document.getStatus() != AbstractDocument.DOC_UNLOCKED)
				throw new PersistenceException(
						String.format("Document %s is already locked by user %s and cannot be locked by %s", document,
								document.getLockUser(), transaction.getUser().getFullName()));

			documentDAO.initialize(document);
			document.setLockUserId(transaction.getUser().getId());
			document.setLockUser(transaction.getUser().getFullName());
			document.setStatus(status);
			document.setFolder(document.getFolder());

			if (transaction.getEvent() == null)
				transaction.setEvent(DocumentEvent.LOCKED.toString());

			// Modify document history entry
			documentDAO.store(document, transaction);
		}

		log.debug("locked document {}", docId);
	}

	private void storeFile(Document doc, File file) throws IOException {
		String resource = store.getResourceName(doc, null, null);
		store.store(file, doc.getId(), resource);
	}

	/**
	 * Utility method for document removal from index
	 * 
	 * @param doc the document to delete from the index
	 */
	@Override
	public void deleteFromIndex(Document doc) {
		try {
			long docId = doc.getId();

			// Physically remove the document from full-text index
			indexer.deleteHit(docId);

			doc.setIndexed(AbstractDocument.INDEX_TO_INDEX);
			documentDAO.store(doc);

			markAliasesToIndex(doc.getId());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public String parseDocument(Document doc, String fileVersion) throws ParsingException {
		String content = null;

		// Check if the document is an alias
		if (doc.getDocRef() != null) {
			long docref = doc.getDocRef();
			try {
				doc = documentDAO.findById(docref);
				if (doc == null)
					throw new ParsingException(String.format("Unexisting referenced document %s", docref));
			} catch (ParsingException pe) {
				throw pe;
			} catch (PersistenceException e) {
				throw new ParsingException(e.getMessage(), e);
			}
		}

		// Parses the file where it is already stored
		Locale locale = doc.getLocale();
		String resource = store.getResourceName(doc, fileVersion, null);
		Parser parser = ParserFactory.getParser(doc.getFileName());

		// and gets some fields
		if (parser != null) {
			log.debug("Using parser {} to parse document {}", parser.getClass().getName(), doc.getId());

			TenantDAO tDao = Context.get(TenantDAO.class);
			try {
				content = parser.parse(store.getStream(doc.getId(), resource), new ParseParameters(doc,
						doc.getFileName(), fileVersion, null, locale, tDao.findById(doc.getTenantId()).getName()));
			} catch (Exception e) {
				log.error("Cannot parse document {}", doc);
				log.error(e.getMessage(), e);
				if (e instanceof ParsingException pe)
					throw pe;
				else
					throw new ParsingException(e);
			}
		}

		if (content == null) {
			content = "";
		}

		return content;
	}

	@Override
	public long index(long docId, String content, DocumentHistory transaction)
			throws PersistenceException, ParsingException {
		Document doc = getExistingDocument(docId);

		log.debug("Indexing document {} - {}", doc.getId(), doc.getFileName());
		int currentIndexed = doc.getIndexed();

		long parsingTime;
		String cont;
		try {
			cont = content;
			parsingTime = 0;
			if (doc.getDocRef() != null) {
				// We are indexing an alias, so index the real document first
				Document realDoc = documentDAO.findById(doc.getDocRef());
				if (realDoc != null) {
					if (realDoc.getIndexed() == AbstractDocument.INDEX_TO_INDEX
							|| realDoc.getIndexed() == AbstractDocument.INDEX_TO_INDEX_METADATA)
						parsingTime = index(realDoc.getId(), content, new DocumentHistory(transaction));

					// Take the content from the real document to avoid double
					// parsing
					if (StringUtils.isEmpty(content))
						cont = indexer.getHit(realDoc.getId()).getContent();
				} else {
					log.debug("Alias {} cannot be indexed because it references an unexisting document {}", doc,
							doc.getDocRef());
					documentDAO.jdbcUpdate(UPDATE_LD_DOCUMENT_SET_LD_INDEXED + AbstractDocument.INDEX_SKIP
							+ " where ld_id=" + doc.getId());
					return 0;
				}
			}

			if (StringUtils.isEmpty(cont) && doc.getIndexed() != AbstractDocument.INDEX_TO_INDEX_METADATA) {
				// Extracts the content from the file. This may take very long
				// time.
				Date beforeParsing = new Date();
				cont = parseDocument(doc, null);
				parsingTime = TimeDiff.getTimeDifference(beforeParsing, new Date(), TimeField.MILLISECOND);
			}

			documentDAO.initialize(doc);

			// This may take time
			addHit(doc, cont);
		} catch (PersistenceException | ParsingException e) {
			recordIndexingError(transaction, doc, e);
			throw e;
		}

		// For additional safety update the DB directly
		doc.setIndexed(AbstractDocument.INDEX_INDEXED);
		documentDAO.jdbcUpdate(UPDATE_LD_DOCUMENT_SET_LD_INDEXED + doc.getIndexed() + " where ld_id=" + doc.getId());

		// Save the event
		if (transaction != null) {
			transaction.setEvent(DocumentEvent.INDEXED.toString());
			transaction.setComment(HTMLSanitizer.sanitize(StringUtils.abbreviate(cont, 100)));
			transaction.setReason(Integer.toString(currentIndexed));
			transaction.setDocument(doc);
		}
		DocumentHistoryDAO hDao = Context.get(DocumentHistoryDAO.class);
		hDao.store(transaction);

		/*
		 * Mark the aliases to be re-indexed
		 */
		markAliasesToIndex(docId);

		return parsingTime;
	}

	private void recordIndexingError(DocumentHistory transaction, Document document, Exception exception)
			throws PersistenceException {
		if (transaction == null)
			return;

		transaction.setEvent(DocumentEvent.INDEXED_ERROR.toString());
		transaction.setComment(exception.getMessage());
		transaction.setDocument(document);
		transaction.setPath(folderDAO.computePathExtended(document.getFolder().getId()));
		DocumentHistoryDAO hDao = Context.get(DocumentHistoryDAO.class);
		hDao.store(transaction);

		if (exception instanceof ParsingException) {
			TenantDAO tDao = Context.get(TenantDAO.class);
			String tenant = tDao.getTenantName(document.getTenantId());
			if (Context.get().getProperties().getBoolean(tenant + ".index.skiponerror", false)) {
				DocumentDAO dDao = Context.get(DocumentDAO.class);
				dDao.initialize(document);
				document.setIndexed(AbstractDocument.INDEX_SKIP);
				dDao.store(document);
			}
		}
	}

	private Document getExistingDocument(long docId) throws PersistenceException {
		Document doc = documentDAO.findById(docId);
		if (doc == null)
			throw new IllegalArgumentException("Unexisting document with ID: " + docId);
		return doc;
	}

	private void addHit(Document doc, String cont) throws ParsingException {
		try {
			indexer.addHit(doc, cont);
		} catch (Exception e) {
			throw new ParsingException(e.getMessage(), e);
		}
	}

	private void markAliasesToIndex(long referencedDocId) throws PersistenceException {
		documentDAO.jdbcUpdate(UPDATE_LD_DOCUMENT_SET_LD_INDEXED + AbstractDocument.INDEX_TO_INDEX + " where ld_docref="
				+ referencedDocId + " and not ld_id = " + referencedDocId);
	}

	@Override
	public void update(Document document, Document docVO, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);
		if (document == null)
			throw new IllegalArgumentException("No document has been provided");

		if (docVO == null)
			throw new IllegalArgumentException(NO_VALUE_OBJECT_HAS_BEEN_PROVIDED);

		try {
			/*
			 * Better to synchronize this block because under high
			 * multi-threading may lead to hibernate's sessions rollbacks
			 */
			synchronizedUpdate(document, docVO, transaction);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if (e instanceof PersistenceException pe)
				throw pe;
			else
				throw new PersistenceException(e);
		}
	}

	private synchronized void synchronizedUpdate(Document document, Document docVO, DocumentHistory transaction)
			throws PersistenceException {
		documentDAO.initialize(document);
		if (document.getImmutable() == 0
				|| (document.getImmutable() == 1 && transaction.getUser().isMemberOf(Group.GROUP_ADMIN))) {
			DocumentHistory renameTransaction = checkDocumentRenamed(document, docVO, transaction);

			// Check CustomId uniqueness
			checkCustomIdUniquenesOnUpdate(document, docVO);

			// The document must be re-indexed
			document.setIndexed(AbstractDocument.INDEX_TO_INDEX);

			document.setWorkflowStatus(docVO.getWorkflowStatus());
			document.setColor(docVO.getColor());

			// Save retention policies
			document.setPublished(docVO.getPublished());
			document.setStartPublishing(docVO.getStartPublishing());
			document.setStopPublishing(docVO.getStopPublishing());

			// Intercept locale changes
			if (!document.getLocale().equals(docVO.getLocale())) {
				indexer.deleteHit(document.getId());
				document.setLocale(docVO.getLocale());
			}

			setFileName(document, docVO);

			document.clearTags();
			document.setTags(docVO.getTags());

			setTemplate(document, docVO);

			if (document.getTemplate() == null) {
				document.setOcrTemplateId(null);
				document.setOcrd(0);
			}

			setOcrTemplate(document, docVO);

			setBarcodeTemplate(document, docVO);

			// create a new version
			Version version = Version.create(document, transaction.getUser(), transaction.getComment(),
					DocumentEvent.CHANGED.toString(), false);

			// Modify document history entry
			document.setVersion(version.getVersion());
			if (renameTransaction != null) {
				renameTransaction.setUser(transaction.getUser());
				documentDAO.store(document, renameTransaction);
			} else {
				documentDAO.store(document, transaction);
			}

			versionDAO.store(version);

			markAliasesToIndex(document.getId());
		} else {
			throw new PersistenceException(String.format("Document %s is immutable", document));
		}
	}

	private DocumentHistory checkDocumentRenamed(Document document, Document docVO, DocumentHistory transaction) {
		DocumentHistory renameTransaction = null;
		if (!document.getFileName().equals(docVO.getFileName()) && docVO.getFileName() != null) {
			renameTransaction = new DocumentHistory(transaction);
			renameTransaction.setFilenameOld(document.getFileName());
			renameTransaction.setEvent(DocumentEvent.RENAMED.toString());
		}
		return renameTransaction;
	}

	private void setFileName(Document document, Document docVO) {
		if (StringUtils.isNotEmpty(docVO.getFileName()) && !document.getFileName().equals(docVO.getFileName())) {
			document.setFileName(docVO.getFileName());
		}
	}

	private void setTemplate(Document document, Document docVO) throws PersistenceException {
		Template template = docVO.getTemplate();
		if (template == null && docVO.getTemplateId() != null)
			template = templateDAO.findById(docVO.getTemplateId());

		// Change the template and attributes
		if (template != null) {
			document.setTemplate(template);
			document.setTemplateId(template.getId());
			if (docVO.getAttributes() != null) {
				document.getAttributes().clear();
				for (Map.Entry<String, Attribute> entry : docVO.getAttributes().entrySet())
					document.getAttributes().put(entry.getKey(), entry.getValue());
			}
		} else {
			document.setTemplate(null);
		}
	}

	private void setBarcodeTemplate(Document document, Document docVO) {
		if ((document.getBarcodeTemplateId() == null && docVO.getBarcodeTemplateId() != null)
				|| (document.getBarcodeTemplateId() != null && docVO.getBarcodeTemplateId() == null)
				|| (document.getBarcodeTemplateId() == null && docVO.getBarcodeTemplateId() == null)
				|| !document.getBarcodeTemplateId().equals(docVO.getBarcodeTemplateId()))
			document.setBarcoded(0);
		else
			document.setBarcoded(docVO.getBarcoded());
		document.setBarcodeTemplateId(docVO.getBarcodeTemplateId());
	}

	private void setOcrTemplate(Document document, Document docVO) {
		if ((document.getOcrTemplateId() == null && docVO.getOcrTemplateId() != null)
				|| (document.getOcrTemplateId() != null && docVO.getOcrTemplateId() == null)
				|| (document.getOcrTemplateId() == null && docVO.getOcrTemplateId() == null)
				|| !document.getOcrTemplateId().equals(docVO.getOcrTemplateId()))
			document.setOcrd(0);
		else
			document.setOcrd(docVO.getOcrd());
		document.setOcrTemplateId(docVO.getOcrTemplateId());
	}

	private void checkCustomIdUniquenesOnUpdate(Document document, Document docVO) throws PersistenceException {
		if (docVO.getCustomId() != null) {
			Document test = documentDAO.findByCustomId(docVO.getCustomId(), docVO.getTenantId());
			if (test != null && test.getId() != document.getId())
				throw new PersistenceException("Duplicated CustomID");
			document.setCustomId(docVO.getCustomId());
		}
	}

	@Override
	public void moveToFolder(Document doc, Folder folder, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		if (folder.equals(doc.getFolder()))
			return;

		if (doc.getImmutable() == 0
				|| (doc.getImmutable() == 1 && transaction.getUser().isMemberOf(Group.GROUP_ADMIN))) {

			/*
			 * Better to synchronize this block because under high
			 * multi-threading may lead to hibernate's sessions rollbacks
			 */
			synchronized (this) {
				documentDAO.initialize(doc);
				transaction.setPathOld(folderDAO.computePathExtended(doc.getFolder().getId()));
				transaction.setFilenameOld(doc.getFileName());
				transaction.setEvent(DocumentEvent.MOVED.toString());

				doc.setFolder(folder);

				// The document needs to be reindexed
				if (doc.getIndexed() == AbstractDocument.INDEX_INDEXED) {
					doc.setIndexed(AbstractDocument.INDEX_TO_INDEX);
					indexer.deleteHit(doc.getId());

					// The same thing should be done on each shortcut
					documentDAO.jdbcUpdate(UPDATE_LD_DOCUMENT_SET_LD_INDEXED + AbstractDocument.INDEX_TO_INDEX
							+ " where ld_docref=" + doc.getId());
				}

				// Modify document history entry
				if (transaction.getEvent().trim().isEmpty())
					transaction.setEvent(DocumentEvent.MOVED.toString());

				Version version = Version.create(doc, transaction.getUser(), transaction.getComment(),
						DocumentEvent.MOVED.toString(), false);
				version.setId(0);

				documentDAO.store(doc, transaction);

				storeVersionAsync(version);
			}
		} else {
			throw new PersistenceException(DOCUMENT_IS_IMMUTABLE);
		}
	}

	@Override
	public Document create(InputStream content, Document docVO, DocumentHistory transaction)
			throws PersistenceException {
		if (transaction == null)
			throw new IllegalArgumentException("No transaction has been specified");

		if (docVO == null)
			throw new IllegalArgumentException(NO_VALUE_OBJECT_HAS_BEEN_PROVIDED);

		// Write content to temporary file, then delete it
		File tmp = null;
		try {
			tmp = FileUtil.createTempFile("create", "");
			if (content != null)
				FileUtil.writeFile(content, tmp.getPath());
			return create(tmp, docVO, transaction);
		} catch (PersistenceException pe) {
			throw pe;
		} catch (Exception ioe) {
			throw new PersistenceException(ioe.getMessage(), ioe);
		} finally {
			FileUtil.delete(tmp);
		}
	}

	@Override
	public Document create(File file, Document docVO, DocumentHistory transaction) throws PersistenceException {
		if (transaction == null)
			throw new IllegalArgumentException(TRANSACTION_CANNOT_BE_NULL);

		if (docVO == null)
			throw new IllegalArgumentException(NO_VALUE_OBJECT_HAS_BEEN_PROVIDED);

		if (!(file != null && file.length() > 0))
			throw new IllegalArgumentException("Cannot create 0 bytes document");

		setAtributesForCreation(file, docVO, transaction);

		/*
		 * Better to synchronize this block because under high multi-threading
		 * it may lead to hibernate's sessions rollbacks
		 */
		synchronized (this) {
			countPages(file, docVO);

			if (docVO.getTemplate() == null && docVO.getTemplateId() != null)
				docVO.setTemplate(templateDAO.findById(docVO.getTemplateId()));

			transaction.setFile(file.getAbsolutePath());

			// Create the gridRecord
			transaction.setEvent(DocumentEvent.STORED.toString());
			documentDAO.store(docVO, transaction);

			/* store the document into filesystem */
			try {
				storeFile(docVO, file);
			} catch (Exception e) {
				documentDAO.delete(docVO.getId());
				throw new PersistenceException(String.format("Unable to store the file of document %d", docVO.getId()),
						e);
			}

			// The document record has been written, now store the initial
			// version (default 1.0)
			Version version = Version.create(docVO, userDAO.findById(transaction.getUserId()), transaction.getComment(),
					DocumentEvent.STORED.toString(), true);

			storeVersionAsync(version);

			return docVO;
		}

	}

	/**
	 * Saves a version in another thread waiting for the referenced document to
	 * be available into the database.
	 * 
	 * @param version the version to save
	 */
	void storeVersionAsync(Version version) {
		/*
		 * Probably the document's record has not been written yet, we should
		 * fork a thread to wait for it's write.
		 */
		ThreadPools.get().schedule(() -> {
			try {
				// Wait for the document's record write
				String documentWriteCheckQuery = "select count(*) from ld_document where ld_id=" + version.getDocId();
				int count = 0;
				int tests = 0;
				while (count == 0 && tests < 100) {
					count = documentDAO.queryForInt(documentWriteCheckQuery);
					Thread.sleep(1000L);
					tests++;
				}

				if (count > 0) {
					if (log.isDebugEnabled())
						log.debug("Record of document {} has been written", version.getDocId());

					versionDAO.store(version);

					if (log.isDebugEnabled())
						log.debug("Stored version {} of document {}", version.getVersion(), version.getDocId());
				}
			} catch (PersistenceException ex) {
				log.error(ex.getMessage(), ex);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}, "VersionSave", 100L);
	}

	private void setAtributesForCreation(File file, Document docVO, DocumentHistory transaction) {
		String type = UNKNOWN;
		int lastDotIndex = docVO.getFileName().lastIndexOf(".");
		if (lastDotIndex > 0) {
			type = FileUtil.getExtension(docVO.getFileName()).toLowerCase();
		}

		if (docVO.getDate() == null)
			docVO.setDate(new Date());

		if (docVO.getCreation() == null)
			docVO.setCreation(docVO.getDate());

		if (StringUtils.isNotEmpty(docVO.getPublisher()))
			docVO.setPublisher(docVO.getPublisher());
		else
			docVO.setPublisher(transaction.getUsername());

		if (docVO.getPublisherId() != 0L)
			docVO.setPublisherId(docVO.getPublisherId());
		else
			docVO.setPublisherId(transaction.getUserId());

		if (StringUtils.isNotEmpty(docVO.getCreator()))
			docVO.setCreator(docVO.getCreator());
		else
			docVO.setCreator(transaction.getUsername());

		if (docVO.getCreatorId() != 0L)
			docVO.setCreatorId(docVO.getCreatorId());
		else
			docVO.setCreatorId(transaction.getUserId());

		docVO.setStatus(AbstractDocument.DOC_UNLOCKED);
		docVO.setType(type);
		docVO.setVersion(config.getProperty("document.startversion"));
		docVO.setFileVersion(docVO.getVersion());
		docVO.setFileSize(file.length());
		docVO.setId(0L);
	}

	/**
	 * Processes a file trying to calculate the pages and updates the pages
	 * property of the given document.
	 * 
	 * @param file The document's file
	 * @param doc The document
	 */
	private void countPages(File file, Document doc) {
		try {
			Parser parser = ParserFactory.getParser(doc.getFileName());
			if (parser != null) {
				log.debug("Using parser {} to count pages of document {}", parser.getClass().getName(), doc);
				doc.setPages(parser.countPages(file, doc.getFileName()));
			}
		} catch (Exception e) {
			log.warn("Cannot count pages of document {}", doc, e);
		}
	}

	@Override
	public int countPages(Document doc) {
		try {
			Parser parser = ParserFactory.getParser(doc.getFileName());
			Store strt = Context.get(Store.class);
			return parser.countPages(strt.getStream(doc.getId(), strt.getResourceName(doc, null, null)),
					doc.getFileName());
		} catch (Exception e) {
			log.warn("Cannot count pages of document {}", doc, e);
			return 1;
		}
	}

	public Document copyToFolder(Document doc, Folder folder, DocumentHistory transaction, boolean links, boolean notes,
			boolean security) throws PersistenceException, IOException {
		validateTransaction(transaction);

		// initialize the document
		documentDAO.initialize(doc);

		if (doc.getDocRef() != null) {
			return createAlias(doc, folder, doc.getDocRefType(), transaction);
		}

		String resource = store.getResourceName(doc, null, null);
		try (InputStream is = store.getStream(doc.getId(), resource);) {
			Document cloned = new Document(doc);
			cloned.setId(0);
			if (doc.getFolder().getId() != folder.getId())
				cloned.setFolder(folder);
			cloned.setLastModified(null);
			cloned.setDate(null);
			if (cloned.getIndexed() == AbstractDocument.INDEX_INDEXED)
				cloned.setIndexed(AbstractDocument.INDEX_TO_INDEX);
			cloned.setStamped(0);
			cloned.setSigned(0);
			cloned.setLinks(0);
			cloned.setOcrd(0);
			cloned.setBarcoded(0);

			if (!security)
				cloned.getAccessControlList().clear();

			Document createdDocument = create(is, cloned, transaction);

			// Save the event of the copy
			DocumentHistory copyEvent = new DocumentHistory(transaction);
			copyEvent.setDocument(doc);
			copyEvent.setFolder(doc.getFolder());
			copyEvent.setEvent(DocumentEvent.COPYED.toString());

			String newPath = folderDAO.computePathExtended(folder.getId());
			copyEvent.setComment(newPath + "/" + createdDocument.getFileName());
			documentDAO.saveDocumentHistory(doc, copyEvent);

			if (links)
				copyLinks(doc, createdDocument);

			if (notes)
				copyNotes(doc, createdDocument);

			return createdDocument;
		}
	}

	private void copyNotes(Document sourceDocument, Document createdDocument) throws PersistenceException {
		List<DocumentNote> docNotes = documentNoteDAO.findByDocId(sourceDocument.getId(),
				sourceDocument.getFileVersion());
		docNotes.sort((o1, o2) -> o1.getDate().compareTo(o2.getDate()));
		for (DocumentNote docNote : docNotes) {
			DocumentNote newNote = new DocumentNote(docNote);
			newNote.setDocId(createdDocument.getId());
			newNote.setFileVersion(null);

			try {
				documentNoteDAO.store(newNote);
			} catch (PersistenceException e) {
				log.warn("Error copying note {}", docNote);
			}
		}
	}

	private void copyLinks(Document sourceDocument, Document createdDocument) throws PersistenceException {
		List<DocumentLink> docLinks = documentLinkDAO.findByDocId(sourceDocument.getId());

		for (DocumentLink docLink : docLinks) {
			DocumentLink newLink = new DocumentLink();
			newLink.setTenantId(docLink.getTenantId());
			newLink.setType(docLink.getType());
			if (docLink.getDocument1().getId() == sourceDocument.getId()) {
				newLink.setDocument1(createdDocument);
				newLink.setDocument2(docLink.getDocument2());
			} else {
				newLink.setDocument2(createdDocument);
				newLink.setDocument1(docLink.getDocument1());
			}

			documentLinkDAO.store(newLink);
		}
	}

	@Override
	public void unlock(long docId, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		/*
		 * Better to synchronize this block because under high multi-threading
		 * may lead to hibernate's sessions rollbacks
		 */
		synchronized (this) {
			Document document = documentDAO.findDocument(docId);
			documentDAO.initialize(document);

			if (transaction.getUser().isMemberOf(Group.GROUP_ADMIN)) {
				document.setImmutable(0);
			} else if (document.getLockUserId() == null || document.getStatus() == AbstractDocument.DOC_UNLOCKED) {
				log.debug("The document {} is already unlocked", document);
				return;
			} else if (!transaction.getUserId().toString().equals(document.getLockUserId().toString())) {
				/*
				 * We compare the string representations because found that
				 * sometimes the comparison between longs fails
				 */
				String message = String.format("The document %s is locked by %s and cannot be unlocked by %s", document,
						document.getLockUser(), transaction.getUser().getFullName());
				throw new PersistenceException(message);
			}

			document.setLockUserId(null);
			document.setLockUser(null);
			document.setExtResId(null);
			document.setStatus(AbstractDocument.DOC_UNLOCKED);

			// Modify document history entry
			transaction.setEvent(DocumentEvent.UNLOCKED.toString());
			documentDAO.store(document, transaction);
		}
		log.debug("Unlocked document {}", docId);
	}

	@Override
	public void makeImmutable(long docId, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		Document document = documentDAO.findById(docId);
		if (document.getImmutable() == 0) {
			// Modify document history entry
			transaction.setEvent(DocumentEvent.IMMUTABLE.toString());
			documentDAO.makeImmutable(docId, transaction);

			log.debug("The document {} has been marked as immutable", docId);
		} else {
			throw new PersistenceException(DOCUMENT_IS_IMMUTABLE);
		}
	}

	@Override
	public void rename(long docId, String newName, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		/*
		 * Better to synchronize this block because under high multi-threading
		 * may lead to hibernate's sessions rollbacks
		 */
		synchronized (this) {
			Document document = documentDAO.findById(docId);

			if (document.getImmutable() == 0
					|| (document.getImmutable() == 1 && transaction.getUser().isMemberOf(Group.GROUP_ADMIN))) {
				documentDAO.initialize(document);
				document.setFileName(newName.trim());
				String extension = FileUtil.getExtension(newName.trim());
				if (StringUtils.isNotEmpty(extension)) {
					document.setType(FileUtil.getExtension(newName));
				} else {
					document.setType(UNKNOWN);
				}

				document.setIndexed(AbstractDocument.INDEX_TO_INDEX);

				Version version = Version.create(document, transaction.getUser(), transaction.getComment(),
						DocumentEvent.RENAMED.toString(), false);
				storeVersionAsync(version);

				transaction.setEvent(DocumentEvent.RENAMED.toString());
				documentDAO.store(document, transaction);

				markAliasesToIndex(docId);
				log.debug("Document renamed: {}", document.getId());
			} else {
				throw new PersistenceException(DOCUMENT_IS_IMMUTABLE);
			}
		}
	}

	@Override
	public Document replaceAlias(long aliasId, DocumentHistory transaction) throws PersistenceException {
		validateTransaction(transaction);

		// get the alias
		Document alias = documentDAO.findById(aliasId);
		if (alias == null || alias.getDocRef() == null)
			throw new PersistenceException(String.format("Unable to find alias %s", aliasId));

		Folder folder = alias.getFolder();
		folderDAO.initialize(folder);

		if (!folderDAO.isWriteAllowed(alias.getFolder().getId(), transaction.getUserId()))
			throw new PersistenceException(String.format("User %s without WRITE permission in folder %s",
					transaction.getUsername(), folder.getId()));

		Document originalDoc = documentDAO.findById(alias.getDocRef());
		documentDAO.initialize(originalDoc);
		documentDAO.delete(aliasId, transaction);

		try {
			return copyToFolder(originalDoc, folder, new DocumentHistory(transaction), true, true, true);
		} catch (IOException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public Document createAlias(Document doc, Folder folder, String aliasType, DocumentHistory transaction)
			throws PersistenceException {
		if (doc == null)
			throw new IllegalArgumentException("No document has been provided");
		if (folder == null)
			throw new IllegalArgumentException("No folder has been provided");
		validateTransaction(transaction);

		try {
			// initialize the document
			documentDAO.initialize(doc);

			Document alias = new Document();
			alias.setFolder(folder);
			alias.setFileName(doc.getFileName());
			alias.setFileSize(doc.getFileSize());
			alias.setDate(new Date());
			alias.setVersion(doc.getVersion());
			alias.setFileVersion(doc.getFileVersion());

			String type = UNKNOWN;
			int lastDotIndex = doc.getFileName().lastIndexOf(".");
			if (lastDotIndex > 0)
				type = FileUtil.getExtension(doc.getFileName());

			if (StringUtils.isNotEmpty(aliasType)) {
				alias.setFileName(
						FileUtil.getBaseName(doc.getFileName()) + "." + FileUtil.getExtension(aliasType).toLowerCase());
				type = FileUtil.getExtension(aliasType).toLowerCase();
			}

			alias.setPublisher(transaction.getUsername());
			alias.setPublisherId(transaction.getUserId());
			alias.setCreator(transaction.getUsername());
			alias.setCreatorId(transaction.getUserId());
			alias.setStatus(AbstractDocument.DOC_UNLOCKED);
			alias.setType(type);

			// Set the Doc Reference
			if (doc.getDocRef() == null) {
				// Set the docref as the id of the original document
				alias.setDocRef(doc.getId());
			} else {
				// The doc is a shortcut, so we still copy a shortcut
				alias.setDocRef(doc.getDocRef());
			}
			alias.setDocRefType(aliasType);

			// Modify document history entry
			transaction.setEvent(DocumentEvent.SHORTCUT_STORED.toString());

			documentDAO.store(alias, transaction);

			return alias;
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void changeIndexingStatus(Document doc, int status) {
		if (status == AbstractDocument.INDEX_SKIP && doc.getIndexed() == AbstractDocument.INDEX_SKIP)
			return;
		if (status == AbstractDocument.INDEX_TO_INDEX && doc.getIndexed() == AbstractDocument.INDEX_TO_INDEX)
			return;
		if (status == AbstractDocument.INDEX_TO_INDEX_METADATA
				&& doc.getIndexed() == AbstractDocument.INDEX_TO_INDEX_METADATA)
			return;

		documentDAO.initialize(doc);
		if (doc.getIndexed() == AbstractDocument.INDEX_INDEXED)
			deleteFromIndex(doc);
		doc.setIndexed(status);
		try {
			documentDAO.store(doc);
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Version deleteVersion(long versionId, DocumentHistory transaction) throws PersistenceException {
		Version versionToDelete = enforceExistingVersion(versionId);

		String versionToDeleteSpec = versionToDelete.getVersion();

		Document document = enforceExistingDocument(versionToDelete.getDocId());

		List<Version> versions = versionDAO.findByDocId(versionToDelete.getDocId());

		// Exit if there is only one version
		if (versions.size() == 1)
			return versions.get(0);

		// Iterate over the versions to check if the file is referenced by other
		// versions
		boolean referenced = false;
		for (Version v : versions)
			if (v.getId() != versionId && versionToDelete.getFileVersion().equals(v.getFileVersion())) {
				referenced = true;
				break;
			}

		// If no more referenced, can delete the document's resources
		if (!referenced) {
			List<String> resources = store.listResources(versionToDelete.getDocId(), versionToDelete.getFileVersion());
			for (String resource : resources)
				try {
					store.delete(versionToDelete.getDocId(), resource);
				} catch (Exception t) {
					log.warn("Unable to delete resource {} of document {}", resource, versionToDelete.getDocId());
				}
		} else {
			log.warn("Cannot delete version {} of document {} because file version {} is still referenced",
					versionToDelete.getVersion(), versionToDelete.getFileVersion(), versionToDelete.getDocId());
		}

		try {
			versionDAO.delete(versionId);
		} catch (PersistenceException e) {
			throw new PersistenceException("Version not deleted from the database", e);
		}

		// Save the version deletion history
		DocumentHistory delHistory = null;
		if (transaction != null) {
			delHistory = new DocumentHistory(transaction);
			delHistory.setEvent(DocumentEvent.VERSION_DELETED.toString());
			delHistory.setComment(versionToDeleteSpec + " - " + versionToDelete.getFileVersion());
		}
		documentDAO.saveDocumentHistory(document, delHistory);

		versions = versionDAO.findByDocId(versionToDelete.getDocId());

		Version lastVersion = getLastVersion(versions, versionToDelete);

		/*
		 * Downgrade the document version in case the deleted version is the
		 * current one
		 */
		downgradeDocumentVersion(document, versionToDeleteSpec, transaction, lastVersion);

		return lastVersion;
	}

	protected Document enforceExistingDocument(long docId) throws PersistenceException {
		Document document = documentDAO.findById(docId);
		if (document == null)
			throw new IllegalArgumentException("Unexisting referenced document " + docId);
		return document;
	}

	protected Version enforceExistingVersion(long versionId) throws PersistenceException {
		Version versionToDelete = versionDAO.findById(versionId);
		if (versionToDelete == null)
			throw new IllegalArgumentException("Unexisting version " + versionId);
		return versionToDelete;
	}

	private Version getLastVersion(List<Version> versions, Version versionToDelete) {
		Version lastVersion = null;
		for (Version version : versions) {
			if (version.getDeleted() == 0 && version.getId() != versionToDelete.getId()) {
				lastVersion = version;
				break;
			}
		}
		return lastVersion;
	}

	private void downgradeDocumentVersion(Document document, String versionToDeleteSpec, DocumentHistory transaction,
			Version lastVersion) throws PersistenceException {
		String currentVersion = document.getVersion();
		if (currentVersion.equals(versionToDeleteSpec) && lastVersion != null) {
			documentDAO.initialize(document);
			document.setVersion(lastVersion.getVersion());
			document.setFileVersion(lastVersion.getFileVersion());

			if (transaction != null) {
				transaction.setEvent(DocumentEvent.CHANGED.toString());
				transaction.setComment(
						"Version changed to " + document.getVersion() + " (" + document.getFileVersion() + ")");
			}

			documentDAO.store(document, transaction);
		}
	}

	@Override
	public long archiveFolder(long folderId, DocumentHistory transaction) throws PersistenceException {
		Folder root = folderDAO.findFolder(folderId);

		Set<Long> archivedDocIds = new HashSet<>();

		Collection<Long> folderIds = folderDAO.findFolderIdByUserIdAndPermission(transaction.getUserId(),
				Permission.ARCHIVE, root.getId(), true);
		for (Long fid : folderIds) {
			String where = " where ld_deleted=0 and not ld_status=" + AbstractDocument.DOC_ARCHIVED
					+ " and ld_folderid=" + fid;
			archivedDocIds.addAll(documentDAO.queryForList("select ld_id from ld_document " + where, Long.class)
					.stream().collect(Collectors.toSet()));
			if (archivedDocIds.isEmpty())
				continue;
			archiveDocuments(archivedDocIds, transaction);
		}

		return archivedDocIds.size();
	}

	@Override
	public void archiveDocuments(Set<Long> docIds, DocumentHistory transaction) throws PersistenceException {
		if (transaction.getUser() == null)
			throw new IllegalArgumentException("transaction user cannot be null");

		List<Long> idsList = new ArrayList<>();
		DocumentDAO dao = Context.get(DocumentDAO.class);
		Collection<Long> folderIds = folderDAO.findFolderIdByUserIdAndPermission(transaction.getUserId(),
				Permission.ARCHIVE, null, true);

		for (long id : docIds) {
			Document doc = dao.findById(id);

			// Skip documents in folders without Archive permission
			if (!(transaction.getUser().isMemberOf(Group.GROUP_ADMIN)
					|| transaction.getUser().getUsername().equals("_retention"))
					&& !folderIds.contains(doc.getFolder().getId()))
				continue;

			// Create the document history event
			DocumentHistory t = new DocumentHistory(transaction);
			dao.archive(id, t);
			idsList.add(id);
		}

		// Remove all corresponding hits from the index
		SearchEngine engine = Context.get(SearchEngine.class);
		engine.deleteHits(idsList);

		log.info("Archived documents {}", idsList);
	}

	@Override
	public Ticket createTicket(Ticket ticket, DocumentHistory transaction)
			throws PersistenceException, PermissionException {
		validateTransaction(transaction);

		Document document = documentDAO.findById(ticket.getDocId());
		if (document == null)
			throw new PersistenceException("Unexisting document " + ticket.getDocId());

		if (!folderDAO.isDownloadllowed(document.getFolder().getId(), transaction.getUserId()))
			throw new PermissionException(transaction.getUsername(), "Folder " + document.getFolder().getId(),
					Permission.DOWNLOAD);

		ticket.setUserId(transaction.getUserId());

		Calendar cal = Calendar.getInstance();
		if (ticket.getExpired() != null) {
			cal.setTime(ticket.getExpired());
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			ticket.setExpired(cal.getTime());
		} else if (ticket.getExpireHours() != null) {
			cal.add(Calendar.HOUR_OF_DAY, ticket.getExpireHours().intValue());
			ticket.setExpired(cal.getTime());
		} else {
			cal.add(Calendar.HOUR_OF_DAY, config.getInt("ticket.ttl"));
			ticket.setExpired(cal.getTime());
		}

		transaction.setEvent(DocumentEvent.TICKET_CREATED.toString());
		transaction.setSessionId(transaction.getSessionId());

		ticketDAO.store(ticket, transaction);

		// Try to clean the DB from old tickets
		ticketDAO.deleteExpired();

		ticket.setUrl(composeTicketUrl(ticket, ticket.getUrl()));

		return ticket;
	}

	private String composeTicketUrl(Ticket ticket, String urlPrefix) {
		if (StringUtils.isEmpty(urlPrefix))
			urlPrefix = config.getProperty("server.url");
		if (!urlPrefix.endsWith("/"))
			urlPrefix += "/";
		if (ticket.getType() == Ticket.VIEW)
			return urlPrefix + "view/" + ticket.getTicketId();
		else
			return urlPrefix + "download-ticket?ticketId=" + ticket.getTicketId();
	}

	@Override
	public boolean unprotect(String sid, long docId, String password) {
		Session session = SessionManager.get().get(sid);
		if (!session.isOpen())
			return false;

		if (session.getUnprotectedDocs().containsKey(docId))
			return session.getUnprotectedDocs().get(docId).equals(password);
		try {
			Document doc = documentDAO.findDocument(docId);
			if (!doc.isPasswordProtected())
				return true;

			boolean granted = doc.isGranted(password);
			if (granted)
				session.getUnprotectedDocs().put(docId, password);
			return granted;
		} catch (PersistenceException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public void promoteVersion(long docId, String version, DocumentHistory transaction)
			throws PersistenceException, IOException {
		validateTransaction(transaction);

		transaction.setComment(String.format("promoted version %s", version));

		// identify the document and folder
		Document document = documentDAO.findDocument(docId);
		if (document.getImmutable() == 0 && document.getStatus() == AbstractDocument.DOC_UNLOCKED) {
			Version ver = versionDAO.findByVersion(document.getId(), version);
			if (ver == null)
				throw new PersistenceException(String.format("Unexisting version %s of document %d", version, docId));
			versionDAO.initialize(ver);

			transaction.setEvent(DocumentEvent.CHECKEDOUT.toString());
			checkout(document.getId(), transaction);

			// Write the version file into a temporary file
			File tmp = FileUtil.createTempFile("promotion", "");
			try {
				Folder originalFolder = document.getFolder();
				Version docVO = new Version(ver);
				docVO.setFolder(originalFolder);
				docVO.setCustomId(ver.getCustomId());
				docVO.setId(0L);

				if (ver.getTemplateId() != null)
					docVO.setTemplate(templateDAO.findById(ver.getTemplateId()));

				if (StringUtils.isNotEmpty(ver.getTgs())) {
					Set<String> tags = Arrays.asList(ver.getTgs().split(",")).stream().collect(Collectors.toSet());
					docVO.setTagsFromWords(tags);
				}

				store.writeToFile(document.getId(), store.getResourceName(document, ver.getFileVersion(), null), tmp);
				DocumentHistory checkinTransaction = new DocumentHistory(transaction);
				checkinTransaction.setDate(new Date());
				checkin(document.getId(), tmp, ver.getFileName(), false, docVO, checkinTransaction);

				log.debug("Promoted version {} of document {}", version, docId);
			} finally {
				FileUtils.deleteQuietly(tmp);
			}
		}
	}

	@Override
	public int enforceFilesIntoFolderStore(long rootFolderId, DocumentHistory transaction)
			throws PersistenceException, IOException {
		Folder rootFolder = folderDAO.findFolder(rootFolderId);
		if (rootFolder == null)
			throw new PersistenceException("Unexisting folder ID  " + rootFolderId);

		if (transaction != null)
			transaction.setEvent(DocumentEvent.CHANGED.toString());

		int totalMovedFiles = 0;

		// Traverse the tree
		Collection<Long> folderIds = folderDAO.findFolderIdInTree(rootFolderId, false);
		for (Long folderId : folderIds) {
			Folder folder = folderDAO.findById(folderId);
			if (folder == null || folder.getFoldRef() != null)
				continue;

			folderDAO.initialize(folder);

			// Retrieve the store specification from the current folder
			int targetStore = getStore(folder);

			log.info("Move the files of all the documents inside the folder {} into the target store {}", rootFolder,
					targetStore);

			List<Document> documents = documentDAO.findByFolder(folderId, null);

			for (Document document : documents) {
				int movedFiles = store.moveResourcesToStore(document.getId(), targetStore);

				if (movedFiles > 0) {
					totalMovedFiles += movedFiles;
					try {
						DocumentHistory storedTransaction = new DocumentHistory(transaction);
						storedTransaction
								.setComment(String.format("%d files moved to store %d", movedFiles, targetStore));
						documentDAO.saveDocumentHistory(document, transaction);
					} catch (Exception t) {
						log.warn("Cannot gridRecord history for document {}", document, t);
					}
				}
			}
		}

		return totalMovedFiles;
	}

	private int getStore(Folder folder) {
		int targetStore = config.getInt("store.write", 1);
		if (folder.getStore() != null)
			targetStore = folder.getStore().intValue();
		else {
			try {
				// Check if one of the parent folders references the store
				List<Folder> parents = folderDAO.findParents(folder.getId());
				Collections.reverse(parents);

				for (Folder parentFolder : parents) {
					folderDAO.initialize(parentFolder);
					if (parentFolder.getStore() != null) {
						targetStore = parentFolder.getStore().intValue();
						break;
					}
				}
			} catch (PersistenceException e) {
				log.error(e.getMessage(), e);
			}
		}
		return targetStore;
	}

	@Override
	public Document merge(Collection<Document> documents, long targetFolderId, String fileName,
			DocumentHistory transaction) throws IOException, PersistenceException {
		List<Long> docIds = documents.stream().map(d -> d.getId()).toList();
		File tempDir = null;
		File bigPdf = null;
		try {
			tempDir = preparePdfs(transaction != null ? transaction.getUser() : null, docIds);

			// Now collect and sort each PDF
			File[] pdfs = tempDir.listFiles();

			Arrays.sort(pdfs, (o1, o2) -> o1.getName().compareTo(o2.getName()));

			// Merge all the PDFs
			bigPdf = MergeUtil.mergePdf(List.of(pdfs));

			// Add an history entry to track the export of the document
			DocumentDAO docDao = Context.get(DocumentDAO.class);
			if (transaction != null)
				for (Long id : docIds) {
					DocumentHistory trans = new DocumentHistory(transaction);
					trans.setEvent(DocumentEvent.EXPORTPDF.toString());
					docDao.saveDocumentHistory(docDao.findById(id), trans);
				}

			Document docVO = new Document();
			docVO.setFileName(fileName.toLowerCase().endsWith(".pdf") ? fileName : fileName + ".pdf");
			FolderDAO folderDao = Context.get(FolderDAO.class);
			docVO.setFolder(folderDao.findById(targetFolderId));

			DocumentManager manager = Context.get(DocumentManager.class);
			return manager.create(bigPdf, docVO, transaction);
		} finally {
			FileUtil.delete(bigPdf);
			FileUtil.delete(tempDir);
		}
	}

	/**
	 * Convert a selection of documents into PDF and stores them in a temporary
	 * folder
	 * 
	 * @param user The current user
	 * @param docIds List of documents to be converted
	 * 
	 * @return The temporary folder
	 * 
	 * @throws IOException
	 */
	private File preparePdfs(User user, List<Long> docIds) throws IOException {
		Path tempPath = Files.createTempDirectory(MERGE);
		File tempDir = tempPath.toFile();

		DecimalFormat nf = new DecimalFormat("00000000");
		int i = 0;
		for (long docId : docIds) {
			try {
				i++;
				DocumentDAO docDao = Context.get(DocumentDAO.class);
				Document document = docDao.findDocument(docId);

				if (document != null && user != null && !user.isMemberOf(Group.GROUP_ADMIN)
						&& !user.isMemberOf("publisher") && !document.isPublishing())
					continue;

				FormatConverterManager manager = Context.get(FormatConverterManager.class);
				manager.convertToPdf(document, null);

				File pdf = new File(tempDir, nf.format(i) + ".pdf");

				manager.writePdfToFile(document, null, pdf, null);
			} catch (Exception t) {
				log.error(t.getMessage(), t);
			}
		}
		return tempDir;
	}

	@Override
	public void destroyDocument(long docId, FolderHistory transaction)
			throws PersistenceException, PermissionException {
		validateTransaction(transaction);

		MenuDAO menuDAO = Context.get(MenuDAO.class);
		if (!menuDAO.isReadEnable(Menu.DESTROY_DOCUMENTS, transaction.getUserId())) {
			String message = "User " + transaction.getUsername() + " cannot access the menu " + Menu.DESTROY_DOCUMENTS;
			throw new PermissionException(message);
		}

		transaction.setDocId(docId);
		transaction.setEvent(FolderEvent.DOCUMENT_DESTROYED.toString());

		log.debug("Destroying document {}", docId);

		// Just retrieve required informations of the document to destroy
		documentDAO.query("select ld_folderid, ld_filename, ld_version, ld_fileversion from ld_document",
				new RowMapper<Document>() {

					@Override
					public Document mapRow(ResultSet rs, int arg1) throws SQLException {
						transaction.setFolderId(rs.getLong(1));
						transaction.setDocId(docId);
						transaction.setFilename(rs.getString(2));
						transaction.setVersion(rs.getString(3));
						transaction.setFileVersion(rs.getString(4));

						Folder folder = folderDAO.findById(transaction.getFolderId());
						if (folder != null)
							transaction.setFolder(folder);

						return null;
					}
				}, 1);

		List<Long> versionIds = documentDAO.queryForList("select ld_id from ld_version where ld_documentid=" + docId,
				Long.class);
		if (!versionIds.isEmpty()) {
			documentDAO.jdbcUpdate("delete from ld_version_ext where ld_versionid in ("
					+ versionIds.stream().map(id -> Long.toString(id)).collect(Collectors.joining(",")) + ")");
		}

		String documentTag = docId + " - " + transaction.getFilename();

		int count = documentDAO.jdbcUpdate("delete from ld_version where ld_documentid = " + docId);
		log.info("Destroyed {} versions of document {}", count, documentTag);

		documentDAO.jdbcUpdate("delete from ld_document_ext where ld_docid = " + docId);

		count = documentDAO.jdbcUpdate("delete from ld_document where ld_docref = " + docId);
		log.info("Destroyed {} aliases of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_tag where ld_docid = " + docId);
		log.info("Destroyed {} tags of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_link where ld_docid1 = " + docId + " or ld_docid2 = " + docId);
		log.info("Destroyed {} links of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_bookmark where ld_type=0 and ld_docid = " + docId);
		log.info("Destroyed {} bookmarks of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_ticket where ld_docid = " + docId);
		log.info("Destroyed {} tickets of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_note where ld_docid = " + docId);
		log.info("Destroyed {} notes of document {}", count, documentTag);

		count = documentDAO.jdbcUpdate("delete from ld_history where ld_docid = " + docId);
		log.info("Destroyed {} histories of document {}", count, documentTag);

		try {
			count = documentDAO.jdbcUpdate("delete from ld_readingrequest where ld_docid = " + docId);
			log.info("Destroyed {} reading requests of document {}", count, documentTag);
		} catch (Exception e) {
			// Ignore because the table may not exist
		}

		documentDAO.jdbcUpdate("delete from ld_document where ld_id = " + docId);
		log.info("Destroyed the record of document {}", documentTag);

		indexer.deleteHit(docId);
		log.info("Destroyed the index entry of document {}", documentTag);

		store.delete(docId);
		log.info("Destroyed the store of document {}", documentTag);

		log.info("Document {} has been completely destroyed", documentTag);

		// Record this destroy event in the parent folder history
		if (transaction.getFolder() != null)
			folderDAO.saveFolderHistory(transaction.getFolder(), transaction);
	}
}