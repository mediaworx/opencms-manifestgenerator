/*
 * This file is part of the OpenCms Module Manifest Generator by mediaworx.
 *
 * For further information about the OpenCms Module Manifest Generator, 
 * please see the project website at GitHub: 
 * https://github.com/mediaworx/opencms-manifestgenerator
 *
 * The OpenCms Module Manifest Generator is used by the OpenCms Plugin for
 * IntelliJ. For further information see the plugin's project site at GitHub:
 * https://github.com/mediaworx/opencms-intellijplugin
 *  
 * Copyright (C) 2012-2014 mediaworx berlin AG (http://www.mediaworx.com)
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 3 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.mediaworx.opencms.moduleutils.manifestgenerator;

import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlFileWriteException;
import com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions.OpenCmsMetaXmlParseException;
import com.mediaworx.xmlutils.XmlHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the manifest.xml for OpenCms modules from meta files (manifest_stub.xml and separate meta files for all
 * files and folders in the VFS). The manifest_stub.xml must contain all module specific data, structured exactly like
 * in a standard OpenCms module manifest, but the files node must be empty.
 * <br />
 * <br />
 * Sample manifest_stub.xml:
 * <pre>
 * &lt;export&gt;
 *     &lt;info&gt;
 *         &lt;creator&gt;Admin&lt;/creator&gt;
 *         &lt;opencms_version&gt;8.5.1&lt;/opencms_version&gt;
 *         &lt;createdate&gt;Tue, 17 Dec 2013 14:04:17 GMT&lt;/createdate&gt;
 *         &lt;infoproject&gt;Offline&lt;/infoproject&gt;
 *         &lt;export_version&gt;7&lt;/export_version&gt;
 *     &lt;/info&gt;
 *     &lt;module&gt;
 *         &lt;name&gt;com.mediaworx.opencms.intellijconnector&lt;/name&gt;
 *         &lt;nicename&gt;&lt;![CDATA[IntelliJ Plugin Connector]]&gt;&lt;/nicename&gt;
 *         &lt;class/&gt;
 *         &lt;description&gt;&lt;![CDATA[Connects the IntelliJ Plugin to OpenCms]]&gt;&lt;/description&gt;
 *         &lt;version&gt;0.2&lt;/version&gt;
 *         &lt;authorname&gt;&lt;![CDATA[Kai Widmann]]&gt;&lt;/authorname&gt;
 *         &lt;authoremail&gt;&lt;![CDATA[widmann@mediaworx.com]]&gt;&lt;/authoremail&gt;
 *         &lt;datecreated/&gt;
 *         &lt;userinstalled/&gt;
 *         &lt;dateinstalled/&gt;
 *         &lt;dependencies/&gt;
 *         &lt;exportpoints&gt;
 *             &lt;exportpoint uri="/system/modules/com.mediaworx.opencms.intellijconnector/classes/" destination="WEB-INF/classes/"/&gt;
 *         &lt;/exportpoints&gt;
 *         &lt;resources&gt;
 *             &lt;resource uri="/system/modules/com.mediaworx.opencms.intellijconnector/"/&gt;
 *         &lt;/resources&gt;
 *         &lt;parameters/&gt;
 *     &lt;/module&gt;
 *     &lt;files/&gt;
 * &lt;/export&gt;
 * </pre>
 * <br />
 * Meta-Files for VFS files must contain all the meta data for the file that is to be included in the manifest,
 * structured like a file node (type != folder) in a standard OpenCms manifest file, plus additional information about
 * the sibling count of the file, all wrapped in a fileinfo node. The file name for the meta file is equal to the VFS
 * file name with ".ocmsfile.xml" added as suffix.
 * <br />
 * <br />
 * Sample meta file for VFS files (test.jsp.ocmsfile.xml):
 * <pre>
 * &lt;fileinfo&gt;
 *     &lt;file&gt;
 *         &lt;source&gt;${source}&lt;/source&gt;
 *         &lt;destination&gt;${destination}&lt;/destination&gt;
 *         &lt;type&gt;jsp&lt;/type&gt;
 *         &lt;uuidstructure&gt;c76b8773-7c44-11e3-92bb-210cc9a3bba6&lt;/uuidstructure&gt;
 *         &lt;uuidresource&gt;c76b8774-7c44-11e3-92bb-210cc9a3bba6&lt;/uuidresource&gt;
 *         &lt;datelastmodified&gt;Mon, 13 Jan 2014 12:13:24 GMT&lt;/datelastmodified&gt;
 *         &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *         &lt;datecreated&gt;Mon, 13 Jan 2014 11:20:59 GMT&lt;/datecreated&gt;
 *         &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *         &lt;flags&gt;0&lt;/flags&gt;
 *         &lt;properties&gt;
 *             &lt;property&gt;
 *                 &lt;name&gt;Title&lt;/name&gt;
 *                 &lt;value&gt;&lt;![CDATA[Test Formatter]]&gt;&lt;/value&gt;
 *             &lt;/property&gt;
 *             &lt;property type="shared"&gt;
 *                 &lt;name&gt;export&lt;/name&gt;
 *                 &lt;value&gt;&lt;![CDATA[false]]&gt;&lt;/value&gt;
 *             &lt;/property&gt;
 *         &lt;/properties&gt;
 *         &lt;relations/&gt;
 *         &lt;accesscontrol&gt;
 *             &lt;accessentry&gt;
 *                 &lt;uuidprincipal&gt;GROUP.Guests&lt;/uuidprincipal&gt;
 *                 &lt;flags&gt;32&lt;/flags&gt;
 *                 &lt;permissionset&gt;
 *                     &lt;allowed&gt;0&lt;/allowed&gt;
 *                     &lt;denied&gt;0&lt;/denied&gt;
 *                 &lt;/permissionset&gt;
 *             &lt;/accessentry&gt;
 *             &lt;accessentry&gt;
 *                 &lt;uuidprincipal&gt;GROUP.Users&lt;/uuidprincipal&gt;
 *                 &lt;flags&gt;36&lt;/flags&gt;
 *                 &lt;permissionset&gt;
 *                     &lt;allowed&gt;23&lt;/allowed&gt;
 *                     &lt;denied&gt;0&lt;/denied&gt;
 *                 &lt;/permissionset&gt;
 *             &lt;/accessentry&gt;
 *             &lt;accessentry&gt;
 *                 &lt;uuidprincipal&gt;USER.Admin&lt;/uuidprincipal&gt;
 *                 &lt;flags&gt;16&lt;/flags&gt;
 *                 &lt;permissionset&gt;
 *                     &lt;allowed&gt;5&lt;/allowed&gt;
 *                     &lt;denied&gt;26&lt;/denied&gt;
 *                 &lt;/permissionset&gt;
 *             &lt;/accessentry&gt;
 *         &lt;/accesscontrol&gt;
 *     &lt;/file&gt;
 *     &lt;siblingcount&gt;1&lt;/siblingcount&gt;
 * &lt;/fileinfo&gt;
 * </pre>
 * <br />
 * Meta-Files for VFS folders must contain all the meta data for the folder that is to be included in the manifest,
 * structured like a file node (type == folder) in a standard OpenCms manifest file. The file name for the meta file
 * is equal to the VFS folder name with ".ocmsfolder.xml" added as suffix.
 * <br />
 * <br />
 * Sample meta file for VFS files (formatter.ocmsfolder.xml):
 * <pre>
 * &lt;file&gt;
 *     &lt;destination&gt;${destination}&lt;/destination&gt;
 *     &lt;type&gt;folder&lt;/type&gt;
 *     &lt;uuidstructure&gt;b9fb9670-7c44-11e3-92bb-210cc9a3bba6&lt;/uuidstructure&gt;
 *     &lt;datelastmodified&gt;Mon, 13 Jan 2014 11:20:43 GMT&lt;/datelastmodified&gt;
 *     &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *     &lt;datecreated&gt;Mon, 13 Jan 2014 11:20:37 GMT&lt;/datecreated&gt;
 *     &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *     &lt;flags&gt;0&lt;/flags&gt;
 *     &lt;properties&gt;
 *         &lt;property&gt;
 *             &lt;name&gt;Title&lt;/name&gt;
 *             &lt;value&gt;&lt;![CDATA[Test Formatters]]&gt;&lt;/value&gt;
 *         &lt;/property&gt;
 *     &lt;/properties&gt;
 *     &lt;relations/&gt;
 *     &lt;accesscontrol/&gt;
 * &lt;/file&gt;
 * </pre>
 * <br />
 * When putting meta files into version control, there may occur a lot of conflicts because of differing dates (and
 * UUIDs) on local development machines. To avoid conflicts manifest data can be stored with variables instead of dates
 * (and UUIDs). The manifest generator replaces these variables with generated values when a manifest is created. The
 * replacement of meta variables can be triggered separately for dates and UUIDs. We don't recommend storing UUIDs as
 * meta variables
 * <br />
 * <br />
 * Sample meta file for VFS files with variables instead of dates and UUIDs (formatter.ocmsfolder.xml):
 * <pre>
 * &lt;file&gt;
 *     &lt;destination&gt;${destination}&lt;/destination&gt;
 *     &lt;type&gt;folder&lt;/type&gt;
 *     &lt;uuidstructure&gt;${uuidstructure}&lt;/uuidstructure&gt;
 *     &lt;datelastmodified&gt;${datelastmodified}&lt;/datelastmodified&gt;
 *     &lt;userlastmodified&gt;Admin&lt;/userlastmodified&gt;
 *     &lt;datecreated&gt;${datecreated}&lt;/datecreated&gt;
 *     &lt;usercreated&gt;Admin&lt;/usercreated&gt;
 *     &lt;flags&gt;0&lt;/flags&gt;
 *     &lt;properties&gt;
 *         &lt;property&gt;
 *             &lt;name&gt;Title&lt;/name&gt;
 *             &lt;value&gt;&lt;![CDATA[Test Formatters]]&gt;&lt;/value&gt;
 *         &lt;/property&gt;
 *     &lt;/properties&gt;
 *     &lt;relations/&gt;
 *     &lt;accesscontrol/&gt;
 * &lt;/file&gt;
 * </pre>
 * <br />
 * mediaworx provides an OpenCms module for pulling the meta files from OpenCms
 * (com.mediaworx.opencms.intellijconnector).<br />
 * <br />
 * Revision 1.7
 */
public class OpenCmsModuleManifestGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(OpenCmsModuleManifestGenerator.class);

	/** File name of the module manifest stub file (standard OpenCms module manifest with an empty files node) */
	private static final String FILENAME_MANIFEST_STUB = "manifest_stub.xml";

	/** File name for the generated module manifest */
	private static final String FILENAME_MANIFEST = "manifest.xml";

	/** File name suffix for VFS folder meta files */
	private static final String FOLDER_META_SUFFIX = ".ocmsfolder.xml";

	/** File name suffix for VFS file meta files */
	private static final String FILE_META_SUFFIX = ".ocmsfile.xml";

	/** XPath pointing to the files node in the manifest stub file */
	private static final String FILES_NODE_XPATH = "/export/files";

	/** XPath pointing to the file node in VFS file meta files */
	private static final String FILE_NODE_XPATH = "/fileinfo/file";

	/** XPath pointing to the siblingcount node in VFS file meta files */
	private static final String SIBLINGCOUNT_NODE_XPATH = "/fileinfo/siblingcount";

	/** XPath pointing to the uuidresource node in VFS file meta files (used to track siblings) */
	private static final String RESOURCEID_NODE_XPATH = "/fileinfo/file/uuidresource";

	/** XPath pointing to the source node in VFS file meta files */
	private static final String SOURCE_NODE_XPATH = "/fileinfo/file/source";

	/** Array of manifest nodes using CDATA sections */
	private static final String[] CDATA_NODES = new String[] { "nicename", "description", "authorname", "authoremail", "value" };

	/** Variable used as placeholder for the source path */
	public static final String META_VAR_SOURCE = "${source}";

	/** Variable used as placeholder for the destination path */
	public static final String META_VAR_DESTINATION = "${destination}";

	/** Variable used as placeholder for the structure UUID */
	public static final String META_VAR_UUIDSTRUCTURE = "${uuidstructure}";

	/** Variable used as placeholder for the resource UUID */
	public static final String META_VAR_UUIDRESOURCE = "${uuidresource}";

	/** Variable used as placeholder for the resource modification date */
	public static final String META_VAR_DATELASTMODIFIED = "${datelastmodified}";

	/** Variable used as placeholder for the resource creation date */
	public static final String META_VAR_DATECREATED = "${datecreated}";

	/** Variable used as placeholder for the manifest's creation date */
	public static final String META_VAR_CREATEDATE = "${createdate}";

	/** The date format to use for resource creation/modification dates, this is exactly like the date format used by OpenCms */
	private static final DateFormat RESOURCE_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	static {
		RESOURCE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * Set used to track resource Ids of resources with siblings (if a resource is referenced by multiple siblings,
	 * only the first file entry pointing to that resource contains a source node)
	 */
	private HashSet<String> handledSiblingResourceIds;

	/**
	 * Xml helper object used for parsing the manifest stub file
	 */
	private XmlHelper xmlHelper;

	/**
	 * Module version to be used in the manifest, ignored if <code>null</code> or empty
	 */
	private String moduleVersion;

	/**
	 * The root path under which the manifest stub and all the meta files are stored
	 */
	private String manifestRootPath;

	/**
	 * The root path under which the real (not meta) files are stored. Used to get the modification date, may be null
	 * (if so the meta file's modification date is used)
	 */
	private String realFileRootPath;

	/**
	 * Flag indicating if date variables (<code>${datelastmodified}</code> and <code>${datecreated}</code>) should be
	 * replaced with generated values.
	 */
	private boolean replaceDateVariables = false;

	/**
	 * Flag indicating if UUID variables (<code>${uuidstructure}</code> and <code>${uuidresource}</code>) should be
	 * replaced with generated values.
	 */
	private boolean replaceIdVariables = false;

	/**
	 * Generates the manifest.xml for OpenCms modules from meta files (manifest_stub.xml and separate meta files for
	 * all
	 * files and folders in the VFS).
	 *
	 * @param manifestRoot file representing the root folder of the manifest meta data (including manifest_stub.xml)
	 * @throws OpenCmsMetaXmlParseException     if the XmlHelper can not be initialized or the manifest stub file or
	 *                                          any
	 *                                          meta file can not be read or parsed
	 * @throws OpenCmsMetaXmlFileWriteException if the resulting manifest file can not be written
	 * @deprecated This method is deprecated and will be removed in a future release. Please use
	 *             {@link #generateManifest(File, String)} instead.
	 */
	public void generateManifest(File manifestRoot) throws OpenCmsMetaXmlParseException, OpenCmsMetaXmlFileWriteException {
		generateManifest(manifestRoot, null);
	}

	/**
	 * Generates the manifest.xml for OpenCms modules from meta files (manifest_stub.xml and separate meta files for all
	 * files and folders in the VFS).
	 * @param manifestRoot  file representing the root folder of the manifest meta data (including manifest_stub.xml)
	 * @param realFileRootPath  root path under which the real files (not the meta files) are stored. Used to get the
	 *                          modification date, may be null (if so the meta file's modification date is used)
	 *
	 * @throws OpenCmsMetaXmlParseException     if the XmlHelper can not be initialized or the manifest stub file or any
	 *                                          meta file can not be read or parsed
	 * @throws OpenCmsMetaXmlFileWriteException if the resulting manifest file can not be written
	 */
	public void generateManifest(File manifestRoot, String realFileRootPath) throws OpenCmsMetaXmlParseException, OpenCmsMetaXmlFileWriteException {

		manifestRootPath = manifestRoot.getPath();
		this.realFileRootPath = realFileRootPath;

		handledSiblingResourceIds = new HashSet<String>();

		String manifestStubPath = manifestRoot.getPath() + File.separator + FILENAME_MANIFEST_STUB;
		String manifestPath = manifestRoot.getPath() + File.separator + FILENAME_MANIFEST;
		LOG.info("manifestStubPath: {}", manifestStubPath);

		Node filesNode;
		Document manifest;
		try {
			xmlHelper = new XmlHelper();
			Map<String,String> replacements = null;
			if (replaceDateVariables) {
				replacements = new HashMap<String, String>();
				replacements.put(META_VAR_CREATEDATE, formatDate((new Date()).getTime()));
			}
			manifest = xmlHelper.parseFile(manifestStubPath, replacements);
			filesNode = xmlHelper.getSingleNodeForXPath(manifest, FILES_NODE_XPATH);
		}
		catch (ParserConfigurationException e) {
			throw new OpenCmsMetaXmlParseException("The XmlHelper could not be initialized", e);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub file could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub xml could not be parsed (parse error)", e);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("The manifest stub xml could not be parsed (xpath error)", e);
		}

		// Regular Expression matching anything but Strings ending with the VFS folder meta file suffix (".ocmsfolder.xml")
		String excludeFolderMetaRegex = "^(?:(?!" + Pattern.quote(FOLDER_META_SUFFIX) + "$).)*$";

		// FileFilter filtering all VFS folder meta files (so only VFS file meta files and folders are included)
		IOFileFilter excludeFolderMetaFilter = new RegexFileFilter(excludeFolderMetaRegex);

		// read all files and folders excluding VFS folder meta files
		Collection<File> files = FileUtils.listFilesAndDirs(manifestRoot, excludeFolderMetaFilter, TrueFileFilter.INSTANCE);

		for (File file : files) {
			if (file.isDirectory()) {
				// exclude the manifest root
				if (file.getPath().equals(manifestRoot.getPath())) {
					continue;
				}
				addFolderToFilesNode(filesNode, file);
			}
			else {
				// exclude the manifest stub file and the manifest file
				if (file.getPath().equals(manifestPath) || file.getPath().equals(manifestStubPath)) {
					continue;
				}
				addFileToFilesNode(filesNode, file);
			}
		}

		// render the xml string
		String manifestString = xmlHelper.getXmlStringFromDocument(manifest, CDATA_NODES);

		// if a specific version is provided, replace the original version
		if (StringUtils.isNotBlank(moduleVersion)) {
			manifestString = manifestString.replaceFirst("<version>[^<]*</version>", "<version>" + Matcher.quoteReplacement(moduleVersion) + "</version>");
		}

		// write the manifest to the disk
		try {
			writeManifest(manifestPath, manifestString);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlFileWriteException("manifest.xml could not be written", e);
		}
	}

	/**
	 * Generates a random UUID
	 * @return a random UUID
	 */
	private static String generateUUID()  {
		return UUID.randomUUID().toString();
	}

	/**
	 * Creates a String from the date that can be used as a resource date in the manifest file
	 * @param millisecondsSinceEpoch the milliseconds since January 1, 1970, 00:00:00 GMT.
	 * @return  formatted date as String
	 */
	private static String formatDate(long millisecondsSinceEpoch) {
		Date date = new Date(millisecondsSinceEpoch);
		return RESOURCE_DATE_FORMAT.format(date);
	}

	/**
	 * Replaces System file separators with a forward slash ("/") that's needed for the VFS
	 * @param path  the path containing system file separators
	 * @return the path with file separators replaced by "/"
	 */
	private String fixVfsFileSeparator(String path) {
		if (!File.separator.equals("/")) {
			path = path.replaceAll(Pattern.quote(File.separator), "/");
		}
		return path;
	}

	private String getFormattedDate(File file) {
		long lastModified = 0;
		if (realFileRootPath != null && realFileRootPath.length() > 0) {
			String metaFilePath = file.getPath();
			String realFilePath = metaFilePath.replace(manifestRootPath, realFileRootPath);
			File realFile = new File(realFilePath);
			if (realFile.exists()) {
				lastModified = realFile.lastModified();
			}
			else {
				LOG.warn(String.format("Error in file date detection: real file not found at %s, using the meta file's date", realFilePath));
			}
		}

		// If no real file was found, use the meta file's date as fallback
		if (lastModified == 0) {
			lastModified = file.lastModified();
		}
		return formatDate(lastModified);
	}

	/**
	 * Adds the meta information for the given folder to the given files node.
	 * @param filesNode the files node the folder meta data is to be added to
	 * @param folder    the folder whose meta data is to be added
	 * @throws OpenCmsMetaXmlParseException if the VFS folder meta file can not be read or parsed
	 */
	private void addFolderToFilesNode(Node filesNode, File folder) throws OpenCmsMetaXmlParseException {
		LOG.debug("folder: {}", folder.getPath());
		String metaXmlFilePath = folder.getPath() + FOLDER_META_SUFFIX;
		LOG.debug("meta folder: {}", metaXmlFilePath);

		String vfsPath = metaXmlFilePath.substring(manifestRootPath.length() + 1, metaXmlFilePath.length() - FOLDER_META_SUFFIX.length());
		vfsPath = fixVfsFileSeparator(vfsPath);
		Map<String,String> replacements = new HashMap<String, String>();
		replacements.put(META_VAR_DESTINATION, vfsPath);

		if (replaceDateVariables) {
			String formattedDate = getFormattedDate(folder);
			replacements.put(META_VAR_DATELASTMODIFIED, formattedDate);
			replacements.put(META_VAR_DATECREATED, formattedDate);
		}
		if (replaceIdVariables) {
			replacements.put(META_VAR_UUIDSTRUCTURE, generateUUID());
		}

		try {
			// append the whole content of the meta file as a child node to the files node
			xmlHelper.appendFileAsNode(filesNode, metaXmlFilePath, replacements);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The file " + metaXmlFilePath + " could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed", e);
		}
	}

	/**
	 * Adds the meta information contained in the file node of the given xml file to the given files node. Siblings are
	 * handled according to OpenCms standard (if multiple siblings are pointing to the same resource, only the first
	 * gets a source node).
	 * @param filesNode the files node the file meta data is to be added to
	 * @param metaFile  the meta file whose meta data (contained in the file node) is to be added
	 * @throws OpenCmsMetaXmlParseException if the folder meta file can not be read or parsed
	 */
	private void addFileToFilesNode(Node filesNode, File metaFile) throws OpenCmsMetaXmlParseException {
		String metaXmlFilePath = metaFile.getPath();
		LOG.debug("meta file:   {}", metaXmlFilePath);

		Document fileMetaInfo = getFileMetaInfoFromXmlFile(metaXmlFilePath, metaFile);
		Node fileNode = getFileNodeFromMetaInfo(fileMetaInfo, metaXmlFilePath);
		int numSiblings = getNumSiblingsForFile(fileMetaInfo, metaXmlFilePath);

		// sibling handling only has to be done if there are at least two siblings
		if (numSiblings >= 2) {
			String resourceId = getResourceIdForFile(fileMetaInfo, metaXmlFilePath);
			// if we encounter a resourceId that already has been handled ...
			if (handledSiblingResourceIds.contains(resourceId)) {
				// ... the source node is removed from the file's xml (that's how OpenCms treats siblings: only
				// the first gets a source node), so the resource is not imported a second time during module import
				removeSourceNodeFromFile(fileNode, metaXmlFilePath);
			}
			else {
				handledSiblingResourceIds.add(resourceId);
			}
		}

		xmlHelper.appendNode(filesNode, fileNode);
	}

	/**
	 * Retrieves the XML Document from the VFS file meta file at the given path.
	 *
	 * @param metaXmlFilePath path pointing to the VFS file meta file
	 * @param metaFile the VFS file meta file
	 * @return the XML Document contained in the meta file
	 * @throws OpenCmsMetaXmlParseException if the VFS file meta file can not be read or parsed
	 */
	private Document getFileMetaInfoFromXmlFile(String metaXmlFilePath, File metaFile) throws OpenCmsMetaXmlParseException {
		Document fileMetaInfo;

		String vfsPath = metaXmlFilePath.substring(manifestRootPath.length() + 1, metaXmlFilePath.length() - FILE_META_SUFFIX.length());
		vfsPath = fixVfsFileSeparator(vfsPath);
		Map<String,String> replacements = new HashMap<String, String>();
		replacements.put(META_VAR_SOURCE, vfsPath);
		replacements.put(META_VAR_DESTINATION, vfsPath);

		if (replaceDateVariables) {
			String formattedDate = getFormattedDate(metaFile);
			replacements.put(META_VAR_DATELASTMODIFIED, formattedDate);
			replacements.put(META_VAR_DATECREATED, formattedDate);
		}
		if (replaceIdVariables) {
			replacements.put(META_VAR_UUIDSTRUCTURE, generateUUID());
			replacements.put(META_VAR_UUIDRESOURCE, generateUUID());
		}
		try {
			fileMetaInfo = xmlHelper.parseFile(metaXmlFilePath, replacements);
		}
		catch (IOException e) {
			throw new OpenCmsMetaXmlParseException("The file " + metaXmlFilePath + " could not be read", e);
		}
		catch (SAXException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed (parse error)", e);
		}
		return fileMetaInfo;
	}

	/**
	 * Retrieves the file node from the VFS file meta XML document (located at the XPath {@link #FILE_NODE_XPATH}).
	 *
	 * @param fileMetaInfo      VFS file meta XML document
	 * @param metaXmlFilePath   path pointing to the VFS file meta file (only used for logging purposes)
	 * @return the file node to be added to the manifest
	 * @throws OpenCmsMetaXmlParseException if the file node can not be found at the expected XPath
	 *                                      (see {@link #FILE_NODE_XPATH})
	 */
	private Node getFileNodeFromMetaInfo(Document fileMetaInfo, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		Node fileNode;

		try {
			fileNode = xmlHelper.getSingleNodeForXPath(fileMetaInfo, FILE_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("The xml from the file " + metaXmlFilePath + " could not be parsed (xpath error)", e);
		}
		return fileNode;
	}

	/**
	 * Retrieves the number of siblings for the VFS file.
	 * @param metaInfo          VFS file meta XML document
	 * @param metaXmlFilePath   path pointing to the VFS file meta file (only used for logging purposes)
	 * @return the number of siblings for the VFS file
	 * @throws OpenCmsMetaXmlParseException if the siblingcount node can not be found at the expected XPath
	 *                                      (see {@link #SIBLINGCOUNT_NODE_XPATH})
	 */
	private int getNumSiblingsForFile(Document metaInfo, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		int numSiblings;
		try {
			numSiblings = xmlHelper.getIntValueForXpath(metaInfo, SIBLINGCOUNT_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine sibling count from " + metaXmlFilePath + " (xpath error)", e);
		}
		catch (NumberFormatException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine sibling count from " + metaXmlFilePath + " (not a number)", e);
		}
		return numSiblings;
	}

	/**
	 * Retrieves the resource Id for the VFS file.
	 * @param metaInfo          VFS file meta XML document
	 * @param metaXmlFilePath   path pointing to the VFS file meta file (only used for logging purposes)
	 * @return the resource Id for the VFS file
	 * @throws OpenCmsMetaXmlParseException if the uuidresource node can not be found at the expected XPath
	 *                                      (see {@link #RESOURCEID_NODE_XPATH})
	 */
	private String getResourceIdForFile(Document metaInfo, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		String resourceId;
		try {
			resourceId = xmlHelper.getStringValueForXpath(metaInfo, RESOURCEID_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't determine resource id from " + metaXmlFilePath + " (xpath error)", e);
		}
		return resourceId;
	}

	/**
	 * Removes the source node from the given file node (used for siblings).
	 * @param fileNode          the file node from which the source node is to be removed
	 * @param metaXmlFilePath   path pointing to the VFS file meta file (only used for logging purposes)
	 * @throws OpenCmsMetaXmlParseException if the source node can not be found at the expected XPath
	 *                                      (see {@link #SOURCE_NODE_XPATH})
	 */
	private void removeSourceNodeFromFile(Node fileNode, String metaXmlFilePath) throws OpenCmsMetaXmlParseException {
		Node sourceNode;
		try {
			sourceNode = xmlHelper.getSingleNodeForXPath(fileNode, SOURCE_NODE_XPATH);
		}
		catch (XPathExpressionException e) {
			throw new OpenCmsMetaXmlParseException("Can't remove sibling's source node from " + metaXmlFilePath + " (xpath error)", e);
		}
		sourceNode.getParentNode().removeChild(sourceNode);
	}

	/**
	 * Gets the path to the meta info file for the VFS file or folder at the given VFS path.
	 * @param manifestRoot  root folder of the manifest meta files
	 * @param vfsPath       VFS path of the given VFS file or folder
	 * @param isFolder      <code>true</code> if the VFS resource is a folder, <code>false</code> otherwise
	 * @return  "[vfsPath].ocmsfile.xml" for files, "[vfsPath].ocmsfolder.xml" for folders
	 */
	public static String getMetaInfoPath(String manifestRoot, String vfsPath, boolean isFolder) {
		return manifestRoot + vfsPath + getMetaInfoSuffix(isFolder);
	}

	/**
	 * Returns the suffix for the meta file depending in <code>isFolder</code>.
	 * @param isFolder <code>true</code> if the suffix for folders should be returned, <code>false</code> otherwise
	 * @return  ".ocmsfolder.xml" ({@link #FOLDER_META_SUFFIX} if <code>isFolder</code> is <code>true</code>,
	 *          ".ocmsfile.xml" ({@link #FILE_META_SUFFIX} otherwise
	 */
	private static String getMetaInfoSuffix(boolean isFolder) {
		return isFolder ? FOLDER_META_SUFFIX : FILE_META_SUFFIX;
	}

	/**
	 * Writes the manifest file to the disk.
	 * @param manifestPath      path to the manifest file
	 * @param manifestString    String content of the manifest file
	 * @throws IOException  if writing to disk fails
	 */
	private void writeManifest(String manifestPath, String manifestString) throws IOException {
		FileUtils.writeStringToFile(new File(manifestPath), manifestString);
	}

	/**
	 * Allows to set a specific module version that is replacing the module version from the manifest stub.
	 * @param moduleVersion the String to be used as the module version, if <code>null</code> or empty the version is
	 *                      not changed
	 */
	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}

	/**
	 * Sets the flag indicating if date variables (<code>${datelastmodified}</code> and <code>${datecreated}</code>)
	 * should be replaced with generated values.
	 * @param replaceDateVariables <code>true</code> if date variables should be replaced, <code>false</code> otherwise
	 */
	public void setReplaceDateVariables(boolean replaceDateVariables) {
		this.replaceDateVariables = replaceDateVariables;
	}

	/**
	 * Sets the flag indicating if UUID variables (<code>${uuidstructure}</code> and <code>${uuidresource}</code>)
	 * should be replaced with generated values.
	 *
	 * @param replaceIdVariables <code>true</code> if UUID variables should be replaced, <code>false</code> otherwise
	 */
	public void setReplaceIdVariables(boolean replaceIdVariables) {
		this.replaceIdVariables = replaceIdVariables;
	}

	/**
	 * Sets the flag indicating if meta variables (<code>${uuidstructure}</code>, <code>${uuidresource}</code>,
	 * <code>${datelastmodified}</code> and <code>${datecreated}</code>) should be replaced with generated values.
	 *
	 * @param replaceMetaVariables <code>true</code> if meta variables should be replaced, <code>false</code> otherwise
	 * @deprecated  Use {@link #setReplaceDateVariables(boolean)} and {@link #setReplaceIdVariables(boolean)} instead,
	 *              may be removed in future versions.
	 */
	public void setReplaceMetaVariables(boolean replaceMetaVariables) {
		this.replaceDateVariables = replaceMetaVariables;
		this.replaceIdVariables = replaceMetaVariables;
	}
}
