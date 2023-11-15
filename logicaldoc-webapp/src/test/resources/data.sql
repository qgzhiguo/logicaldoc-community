insert into ld_attributeset
			(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_name, ld_description, ld_readonly, ld_type, ld_tenantid, ld_recordversion)
values (-1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'default','default',1,0,1,1);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,0,'source', 'Source', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,1,'sourceAuthor', 'Author', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,2,'sourceId', 'Original ID', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,3,'sourceType', 'Type', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,4,'object', 'Object', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,5,'coverage', 'Coverage', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,0,6,'recipient', 'Recipient', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,3,7,'sourceDate', 'Date', 0, -1, 0, 0, 0);
insert into ld_attributeset_ext(ld_attsetid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (-1,0,3,7,'multi', 'Multi', 0, -1, 0, 1, 0);

insert into ld_template
			(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_name, ld_description, ld_readonly, ld_type, ld_tenantid, ld_recordversion)
values (-1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'default','default',0,0,1,1);

insert into ld_template_ext(ld_templateid, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
select -1, ld_mandatory, ld_type, ld_position, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly from ld_attributeset_ext where ld_setid=-1;

insert into ld_user
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_enabled,ld_username,ld_password,ld_name,ld_firstname,ld_street,ld_postalcode,ld_city,ld_country,ld_language,ld_email,ld_telephone,ld_type,ld_passwordchanged,ld_passwordexpires,ld_source,ld_quota,ld_passwordexpired,ld_tenantid,ld_recordversion,ld_enforcewrktime)
values     (2,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'boss','d033e22ae348aeb566fc214aec3585c4da997','Meschieri','Marco','','','','','it','m.meschieri@logicalobjects.it','',0,null,0,0,-1,0,1,1,0);
insert into ld_group
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_tenantid,ld_name,ld_type,ld_recordversion)
values     (-2,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'_user_2',1,1);
insert into ld_usergroup
values (-2,2);

insert into ld_user
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_enabled,ld_username,ld_password,ld_name,ld_firstname,ld_street,ld_postalcode,ld_city,ld_country,ld_language,ld_email,ld_telephone,ld_type,ld_passwordchanged,ld_passwordexpires,ld_source,ld_quota,ld_passwordexpired,ld_tenantid,ld_recordversion,ld_enforcewrktime)
values     (3,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'sebastian','d033e22ae348aeb566fc214aec3585c4da997','Sebastian','Stein','','','','','de','seb_stein@gmx.de','',0,null,0,0,0,0,1,1,0);
insert into ld_group
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_tenantid,ld_name,ld_type,ld_recordversion)
values     (-3,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'_user_3',1,1);
insert into ld_usergroup
values (-3,3);

insert into ld_user
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_enabled,ld_username,ld_password,ld_name,ld_firstname,ld_street,ld_postalcode,ld_city,ld_country,ld_language,ld_email,ld_telephone,ld_type,ld_passwordchanged,ld_passwordexpires,ld_source,ld_quota,ld_passwordexpired,ld_tenantid,ld_recordversion,ld_enforcewrktime)
values     (4,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'author','d033e22ae348aeb566fc214aec3585c4da997','Author','Author','','','','','de','author@acme.com','',0,null,0,0,-1,0,1,1,0);
insert into ld_group
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_tenantid,ld_name,ld_type,ld_recordversion)
values     (-4,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'_user_4',1,1);
insert into ld_usergroup
values (-4,4);

insert into ld_user
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_enabled,ld_username,ld_password,ld_name,ld_firstname,ld_street,ld_postalcode,ld_city,ld_country,ld_language,ld_email,ld_telephone,ld_type,ld_passwordchanged,ld_passwordexpires,ld_source,ld_quota,ld_passwordexpired,ld_tenantid,ld_recordversion,ld_enforcewrktime)
values     (5,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,0,'test','d033e22ae348aeb566fc214aec3585c4da997','test','Test','','','','','de','test@acme.com','',0,null,0,0,-1,0,1,1,0);
insert into ld_group
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_tenantid,ld_name,ld_type,ld_recordversion)
values     (-5,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'_user_5',1,1);
insert into ld_usergroup
values (-5,5);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (99,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'menu.admin1',2,'administration.gif',5,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (2000,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'menu',2,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (-101,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'text',2000,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (-102,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'menu102',-101,'administration.gif',3,1,1,1,1);
insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (-103,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'menu103',-101,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1000,'2008-10-22 00:00:00','2008-10-22 00:00:00',1,'menu.admin.1000',2,'administration.gif',5,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1100,'2008-10-22 00:00:00','2008-10-22 00:00:00',1,'menu.admin.1100',1000,'administration.gif',5,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1200,'2009-10-19 00:00:00','2008-10-22 00:00:00',0,'test',2,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1201,'2009-10-19 00:00:00','2008-10-22 00:00:00',0,'abc',1200,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1202,'2009-10-19 00:00:00','2008-10-22 00:00:00',0,'xyz',1201,'administration.gif',3,1,1,1,1);

insert into ld_menu
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_icon,ld_type,ld_tenantid,ld_recordversion,ld_position,ld_enabled)
values     (1203,'2009-10-19 00:00:00','2008-10-22 00:00:00',0,'qqqq',1201,'administration.gif',3,1,1,1,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (2000,1,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (2000,3,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (101,1,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (102,1,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (103,1,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (101,-4,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (102,-4,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (103,-4,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (103,2,1);

insert into ld_menugroup (ld_menuid, ld_groupid, ld_write) values (99,1,0);

insert into ld_usergroup (ld_userid,ld_groupid) values (3,1);

insert into ld_usergroup (ld_userid,ld_groupid) values (3,2);

insert into ld_usergroup (ld_userid,ld_groupid) values (4,2);

insert into ld_usergroup (ld_userid,ld_groupid) values (5,3);

insert into ld_group
           (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_tenantid,ld_name,ld_type,ld_recordversion)
values     (10,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'testGroup',0,1);


insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (6,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'folder6',5,1,0,1,1,1,0);
insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (7,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'folder7',5,1,0,1,1,1,0);
insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (8,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'folder8',7,1,0,1,1,1,0);
insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (1200,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'test',5,1,0,1,1,1,0);
insert into ld_folder_storage(ld_folderid, ld_nodeid, ld_storageid) values (1200, '', 2);

insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (1201,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'ABC',1200,1,0,1,1,1,0);
insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (1202,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'xyz',1201,1,0,1,1,1,0);
insert into ld_folder (ld_id,ld_lastmodified, ld_creation,ld_deleted,ld_name,ld_parentid,ld_type,ld_templocked,ld_tenantid,ld_recordversion,ld_position,ld_hidden)
values (1204,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'deleted',1201,1,0,1,1,1,0);

insert into ld_foldergroup(ld_folderid, ld_groupid, ld_write , ld_add, ld_security, ld_immutable, ld_delete, ld_rename, ld_import, ld_export, ld_sign, ld_archive, ld_workflow, ld_download, ld_calendar, ld_subscription, ld_print, ld_password, ld_move, ld_email, ld_automation, ld_storage, ld_readingreq)
values (6,2,1,1,0,0,1,1,0,0,0,0,0,1,0,0,1,0,1,1,0,0,0);
insert into ld_foldergroup(ld_folderid, ld_groupid, ld_write , ld_add, ld_security, ld_immutable, ld_delete, ld_rename, ld_import, ld_export, ld_sign, ld_archive, ld_workflow, ld_download, ld_calendar, ld_subscription, ld_print, ld_password, ld_move, ld_email, ld_automation, ld_storage, ld_readingreq)
values (6,3,1,1,0,0,1,1,0,0,0,0,0,1,0,0,1,0,1,1,0,0,0);
insert into ld_foldergroup(ld_folderid, ld_groupid, ld_write , ld_add, ld_security, ld_immutable, ld_delete, ld_rename, ld_import, ld_export, ld_sign, ld_archive, ld_workflow, ld_download, ld_calendar, ld_subscription, ld_print, ld_password, ld_move, ld_email, ld_automation, ld_storage, ld_readingreq)
values (6,-3,1,1,0,0,1,1,0,0,0,0,0,1,0,0,1,0,1,1,0,0,0);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (1,5,'2008-10-22 00:00:00',0,0,'a','1.0','2006-12-19 00:00:00','2006-12-19 00:00:00','myself',1,0,'PDF',3,'en','pippo',1356,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_docref,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (2,5,1,'2008-10-22 00:00:00',0,0,'b','1.0','2006-12-19 00:00:00','2006-12-19 00:00:00','myself',1,0,'PDF',3,'en','pippo',1356,0,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (3,5,'2008-10-22 00:00:00',0,0,'c','1.1','2006-12-19 00:00:00','2006-12-19 00:00:00','myself',1,0,'zip',3,'en','test.zip',1356,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (4,5,'2008-10-22 00:00:00',0,0,'d','1.0','2006-12-19 00:00:00','2006-12-19 00:00:00','myself',1,0,'PDF',3,'en','pippo',1356,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (5,5,'2022-10-21 00:00:00',0,0,'e','1.0','2022-10-21 00:00:00','2022-10-21 00:00:00','myself',1,0,'eml',3,'en','Joyce Jinks shared the Bruce Duo post.eml',19003,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (6,5,'2022-10-22 00:00:00',0,0,'f','1.0','2022-10-22 00:00:00','2022-10-22 00:00:00','myself',1,0,'msg',3,'en','Hurry up! Only a few hours for the Prime Day VGA promos !!!.msg',159744,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_document
           (ld_id,ld_folderid,ld_lastmodified,ld_deleted,ld_immutable,ld_customid,ld_version,ld_fileversion,ld_date,ld_creation,ld_publisher,ld_publisherid,ld_status,ld_type,ld_lockuserid,ld_language,ld_filename,ld_filesize,ld_indexed,ld_signed,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (7,5,'2022-10-22 00:00:00',0,0,'g','1.0','1.0','2022-10-22 00:00:00','2022-10-22 00:00:00','myself',1,0,'eml',3,'en','New error indexing documents.eml',159682,1,0,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_ticket
           (ld_id,ld_lastmodified, ld_deleted,ld_ticketid,ld_docid,ld_userid,ld_type,ld_creation,ld_expired,ld_count,ld_tenantid,ld_recordversion,ld_enabled,ld_views)
values     (1,'2008-10-22 00:00:00',0,'1',1,1,0,'2011-01-01 00:00:00','2011-01-02 00:00:00',0,1,1,1,0);

insert into ld_ticket
           (ld_id,ld_lastmodified, ld_deleted,ld_ticketid,ld_docid,ld_userid,ld_type,ld_creation,ld_expired,ld_count,ld_tenantid,ld_recordversion,ld_enabled,ld_views)
values     (2,'2008-10-22 00:00:00',0,'2',2,3,0,'2011-01-01 00:00:00','2011-01-02 00:00:00',0,1,1,1,0);

insert into ld_ticket
           (ld_id,ld_lastmodified, ld_deleted,ld_ticketid,ld_docid,ld_userid,ld_type,ld_creation,ld_expired,ld_count,ld_tenantid,ld_recordversion,ld_enabled,ld_views)
values     (3,'2008-12-22 00:00:00',0,'3',1,3,0,'2011-01-01 00:00:00','2011-01-02 00:00:00',0,1,1,1,0);

insert into ld_version(ld_id, ld_documentid, ld_version, ld_fileversion, ld_templateid, ld_username, ld_userid, ld_versiondate, ld_comment, ld_lastmodified, ld_deleted, ld_immutable, ld_creation, ld_publisherid, ld_indexed, ld_signed, ld_status, ld_filesize, ld_folderid,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (1,1,'testVer01','fileVer01',5,'testUser',1,'2006-12-19 00:00:00','testComment','2009-02-09 00:00:00',0,0,'2009-02-09 00:00:00',1,0,0,0,0,5,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_version(ld_id, ld_documentid, ld_version, ld_fileversion, ld_templateid, ld_username, ld_userid, ld_versiondate, ld_comment, ld_lastmodified, ld_deleted, ld_immutable, ld_creation, ld_publisherid, ld_indexed, ld_signed, ld_status, ld_filesize, ld_folderid,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (2,1,'testVer02','fileVer02',5,'testUser',1,'2006-12-20 00:00:00','testComment','2009-02-09 00:00:00',0,0,'2009-02-09 00:00:00',1,0,0,0,0,5,'',1,0,0,1,1,1,5,0,0,0,0,1);

insert into ld_version(ld_id, ld_documentid, ld_version, ld_fileversion, ld_filename, ld_templateid, ld_username, ld_userid, ld_versiondate, ld_comment, ld_lastmodified, ld_deleted, ld_immutable, ld_creation, ld_publisherid, ld_indexed, ld_signed, ld_status, ld_filesize, ld_folderid,ld_creator,ld_creatorid,ld_exportstatus,ld_barcoded,ld_published,ld_tenantid,ld_recordversion,ld_pages,ld_stamped,ld_nature,ld_links,ld_ocrd,ld_previewpages)
values     (3,7,'1.0','1.0','test.zip',5,'testUser',1,'2006-12-20 00:00:00','testComment','2009-02-09 00:00:00',0,0,'2009-02-09 00:00:00',1,0,0,0,0,5,'',1,0,0,1,1,1,5,0,0,0,0,1);


INSERT INTO ld_version_ext (ld_versionid, ld_mandatory, ld_type, ld_position, ld_stringvalue, ld_intvalue, ld_doublevalue, ld_datevalue, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_parent, ld_stringvalues, ld_dependson, ld_readonly) 
VALUES (1, 0, 0, 0, 'Karma is a God', NULL, NULL, NULL, 'attr1', NULL, 0, NULL, 0, 0, NULL, NULL, NULL, 0);

INSERT INTO ld_version_ext (ld_versionid, ld_mandatory, ld_type, ld_position, ld_stringvalue, ld_intvalue, ld_doublevalue, ld_datevalue, ld_name, ld_label, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_parent, ld_stringvalues, ld_dependson, ld_readonly) 
VALUES (2, 0, 0, 0, 'Karma is a cat purring in my lap', NULL, NULL, NULL, 'attr1', NULL, 0, NULL, 0, 0, NULL, NULL, NULL, 0);


insert into ld_tag(ld_docid, ld_tenantid, ld_tag)
values     (1, 1,'abc');

insert into ld_tag(ld_docid, ld_tenantid, ld_tag)
values     (1, 1, 'def');

insert into ld_tag(ld_docid, ld_tenantid, ld_tag)
values     (1, 1,'ghi');

insert into ld_tag(ld_docid, ld_tenantid, ld_tag)
values     (2, 1,'ask');

insert into ld_tag(ld_docid, ld_tenantid, ld_tag)
values     (3, 1,'zzz');

insert into ld_history 
				(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_folderid, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_version, ld_notified, ld_new,ld_tenantid,ld_recordversion)
values     (-1,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,5,1,'2006-12-20 00:00:00','author','data test 01','reason test 01','1.0',0,1,1,1);

insert into ld_history 
			    (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_folderid, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_version, ld_notified, ld_new,ld_tenantid,ld_recordversion)
values     (-2,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,2,5,1,'2006-12-25 00:00:00','author','data test 01','reason test 02','1.0',0,1,1,1);

insert into ld_history 
			   (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_folderid, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_version, ld_notified,ld_tenantid,ld_recordversion)
values     (-3,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,null,5,3,'2006-12-27 00:00:00','sebastian','data test 03','reason test 03','1.0',1,1,1);

insert into ld_systemmessage
				(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_author, ld_messagetext, ld_subject, ld_sentdate, ld_datescope, ld_prio, ld_confirmation, ld_lastnotified, ld_status, ld_trials, ld_type, ld_html,ld_tenantid,ld_recordversion)
values     (1,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,'admin','message text1','subject1','2008-10-22 00:00:00',5,3,1,'2009-10-29 00:00:00',0,3,1,1,1,1);

insert into ld_systemmessage
				(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_author, ld_messagetext, ld_subject, ld_sentdate, ld_datescope, ld_prio, ld_confirmation, ld_lastnotified, ld_status, ld_trials, ld_type, ld_html,ld_tenantid,ld_recordversion)
values     (2,'2009-10-29 00:00:00','2008-10-22 00:00:00',0,'admin','message text2','subject2','2009-10-29 00:00:00',5,3,1,'2009-10-29 00:00:00',0,3,1,1,1,1);

insert into ld_systemmessage
				(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_author, ld_messagetext, ld_subject, ld_sentdate, ld_datescope, ld_prio, ld_confirmation, ld_lastnotified, ld_status, ld_trials, ld_type, ld_html,ld_tenantid,ld_recordversion)
values     (3,'2009-10-29 00:00:00','2008-10-22 00:00:00',0,'admin','message text3','subject3','2009-10-29 00:00:00',5,3,1,'2009-10-29 00:00:00',0,3,0,1,1,1);

insert into ld_recipient
			(ld_messageid, ld_name, ld_address, ld_mode, ld_type, ld_read)
values     (1,'sebastian','sebastian','pippo',0,0);

insert into ld_recipient
			(ld_messageid, ld_name, ld_address, ld_mode, ld_type, ld_read)
values     (3,'sebastian','sebastian','CC',0,0)

insert into ld_recipient
			(ld_messageid, ld_name, ld_address, ld_mode, ld_type, ld_read)
values     (1,'marco','marco@acme.com','sms',1,0)

insert into ld_recipient
			(ld_messageid, ld_name, ld_address, ld_mode, ld_type, ld_read)
values     (2,'marco','marco@acme.com','CCN',1,0)

insert into ld_recipient
			(ld_messageid, ld_name, ld_address, ld_mode, ld_type, ld_read)
values     (3,'paperino','topolino','sms',2,0)

insert into ld_link(ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_docid1, ld_docid2,ld_type,ld_tenantid,ld_recordversion)
values   (1,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,2,'test',1,1);
insert into ld_link(ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_docid1, ld_docid2,ld_type,ld_tenantid,ld_recordversion)
values   (2,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,2,1,'xyz',1,1);
insert into ld_link(ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_docid1, ld_docid2,ld_type,ld_tenantid,ld_recordversion)
values   (3,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,2,'xxx',1,1);
insert into ld_link(ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_docid1, ld_docid2,ld_type,ld_tenantid,ld_recordversion)
values   (4,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,2,1,'',1,1);

insert into ld_template (ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_name, ld_description, ld_readonly, ld_type, ld_tenantid,ld_recordversion)
values (5, '2008-11-07 00:00:00', '2008-11-07 00:00:00',0,'test1','test1_desc',0,0,1,1);
insert into ld_template_ext (ld_templateid, ld_mandatory, ld_position, ld_type, ld_stringvalue, ld_name, ld_editor, ld_hidden, ld_multiple, ld_readonly)
values (5, 1, 0, 0, 'val1', 'attr1', 0, 0, 0, 0);

insert into ld_attributeset (ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_name, ld_description, ld_readonly, ld_type, ld_tenantid,ld_recordversion)
values (5, '2008-11-07 00:00:00', '2008-11-07 00:00:00',0,'test1','test1_desc',0,0,1,1);
insert into ld_attributeset_ext (ld_attsetid, ld_mandatory, ld_position, ld_type, ld_stringvalue, ld_name, ld_editor, ld_setid, ld_hidden, ld_multiple, ld_readonly)
values (5, 1, 0, 0, 'val1', 'attr1', 0, 5, 0, 0, 0);

insert into ld_template (ld_id, ld_lastmodified, ld_creation,ld_deleted, ld_name, ld_description, ld_readonly, ld_type, ld_tenantid,ld_recordversion)
values (6, '2008-11-07 00:00:00', '2008-11-07 00:00:00',0,'test2','test2_desc',0,0,1,1);


insert into ld_generic(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_type, ld_subtype, ld_string1, ld_string2, ld_integer1, ld_integer2, ld_double1, ld_double2, ld_date1, ld_date2,ld_tenantid,ld_recordversion)
values(1, '2008-11-19 00:00:00', '2008-11-19 00:00:00',0,'a','a1','str1','str2',0,1,1.5,1.6,'2008-11-20 00:00:00','2008-11-20 00:00:00',1,1);
insert into ld_generic_ext(ld_genid, ld_mandatory, ld_type, ld_position, ld_stringvalue, ld_name, ld_editor, ld_hidden, ld_multiple, ld_readonly)
values(1, 0, 0, 0, 'val1','att1',0,0,0,0);
insert into ld_generic(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_type, ld_subtype, ld_string1, ld_string2, ld_integer1, ld_integer2, ld_double1, ld_double2, ld_date1, ld_date2,ld_tenantid,ld_recordversion)
values(2, '2008-11-19 00:00:00', '2008-11-19 00:00:00',0,'a','a2','str1','str2',10,11,1.5,1.6,'2008-11-20 00:00:00','2008-11-20 00:00:00',1,1);
insert into ld_generic(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_type, ld_subtype, ld_string1, ld_string2, ld_integer1, ld_integer2, ld_double1, ld_double2, ld_date1, ld_date2,ld_tenantid,ld_recordversion)
values(3, '2008-11-19 00:00:00', '2008-11-19 00:00:00',1,'a.3','a2.3','str1','str2',10,11,1.5,1.6,'2008-11-20 00:00:00','2008-11-20 00:00:00',1,1);

insert into ld_user_history 
				(ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_notified,ld_tenantid,ld_recordversion)
values     (1,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'2006-12-20 00:00:00','author','data test 01','reason test 01',0,1,1);

insert into ld_user_history 
			    (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_notified,ld_tenantid,ld_recordversion)
values     (2,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,1,'2006-12-25 00:00:00','author','data test 02','reason test 02',0,1,1);

insert into ld_user_history 
			    (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_userid, ld_date, ld_username, ld_event, ld_comment, ld_notified,ld_tenantid,ld_recordversion)
values     (3,'2008-10-22 00:00:00','2008-10-22 00:00:00',0,3,'2006-12-27 00:00:00','sebastian','data test 03','reason test 03',1,1,1);

insert into ld_note (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_username, ld_userid, ld_date, ld_message,ld_tenantid,ld_recordversion,ld_page,ld_opacity,ld_top,ld_left,ld_width,ld_height,ld_fileversion,ld_linewidth,ld_lineopacity,ld_rotation)
values(1, '2011-04-18 00:00:00', '2011-04-18 00:00:00',0,1,'Admin',1,'2011-04-18 00:00:00','message for note 1',1,1,0,80,0.5,0.5,0.15,0.10,'1.0',1,80,0);

insert into ld_note (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_username, ld_userid, ld_date, ld_message,ld_tenantid,ld_recordversion,ld_page,ld_opacity,ld_top,ld_left,ld_width,ld_height,ld_fileversion,ld_linewidth,ld_lineopacity,ld_rotation)
values(2, '2011-04-18 00:00:00', '2011-04-18 00:00:00',0,1,'Admin',1,'2011-04-18 00:00:00','message for note 2',1,1,0,80,0.5,0.5,0.15,0.10,'1.0',1,80,0);

insert into ld_note (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_username, ld_userid, ld_date, ld_message,ld_tenantid,ld_recordversion,ld_page,ld_opacity,ld_top,ld_left,ld_width,ld_height,ld_fileversion,ld_linewidth,ld_lineopacity,ld_rotation)
values(3, '2011-04-18 00:00:00', '2011-04-18 00:00:00',0,4,'John',3,'2011-04-18 00:00:00','message for note 3',1,1,0,80,0.5,0.5,0.15,0.10,'1.0',1,80,0);

insert into ld_note (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_docid, ld_username, ld_userid, ld_date, ld_message,ld_tenantid,ld_recordversion,ld_page,ld_opacity,ld_top,ld_left,ld_width,ld_height,ld_fileversion,ld_linewidth,ld_lineopacity,ld_rotation)
values(4, '2011-04-18 00:00:00', '2011-04-18 00:00:00',1,1,'Admin',1,'2011-04-18 00:00:00','message for note 4',1,1,0,80,0.5,0.5,0.15,0.10,'1.0',1,80,0);

insert into ld_messagetemplate (ld_id, ld_lastmodified, ld_creation, ld_deleted, ld_name, ld_type, ld_language, ld_subject, ld_body,ld_tenantid,ld_recordversion)
values(500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,0,'test','user','en', '$product','$product',1,1);