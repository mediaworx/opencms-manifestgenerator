opencms-manifestgenerator
=========================

Generates OpenCms module manifests out ouf meta data pulled with the [opencms-ideconnector](https://github.com/mediaworx/opencms-ideconnector).

Have a look at the [JavaDoc](http://mediaworx.github.io/opencms-manifestgenerator/javadoc/) for detailed information.

* Revision 1.10 (2024-09-06): fixing a bug that led to a wrong order in the manifest, so files were imported before
                              folders were created (which led to an exception)
* Revision 1.9  (2024-09-06): updated dependencies (based on the OpenCms 17 versions), update to Java 11
* Revision 1.8  (2024-08-24): debug logging if manifest creation fails due to a StringIndexOutOfBoundsException