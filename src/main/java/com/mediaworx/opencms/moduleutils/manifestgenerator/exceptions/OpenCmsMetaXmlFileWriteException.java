package com.mediaworx.opencms.moduleutils.manifestgenerator.exceptions;

/**
 * Exception used when there's an error writing the manifest file to the target
 */
public class OpenCmsMetaXmlFileWriteException extends Exception {

	/**
	 * Creates a new OpenCmsMetaXmlFileWriteException with a throwable that was causing the exception
	 * @param s description of the error
	 * @param cause the root cause of the Exception
	 */
	public OpenCmsMetaXmlFileWriteException(String s, Throwable cause) {
		super(s, cause);
	}
}
