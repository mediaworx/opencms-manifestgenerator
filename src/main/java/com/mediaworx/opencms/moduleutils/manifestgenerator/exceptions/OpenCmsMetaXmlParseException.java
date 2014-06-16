package com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions;

/**
 * Exception used when there's an error parsing xml for the manifest
 */
public class OpenCmsMetaXmlParseException extends Exception {

	/**
	 * Creates a new OpenCmsMetaXmlParseException with a throwable that was causing the exception
	 * @param s description of the error
	 * @param cause the root cause of the Exception
	 */
	public OpenCmsMetaXmlParseException(String s, Throwable cause) {
		super(s, cause);
	}
}
