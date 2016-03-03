textfairy
=========

Android OCR App

available in the [play store] [p]
[p]: https://play.google.com/store/apps/details?id=com.renard.ocr

Features
--------
* convert images to pdf
* recognize text in images
* basic document management
 * delete
 * edit
 * merge multiple documents into one
 * view table of content

Project Structure
-----------------
* *app* contains the android app code
* *hocr2pdf* contains c++ code to create pdf files
 * is used by the text fairy through a jni wrapper (textfairy/jni)
* *image-processing* contains image processing code (binarization and page segmentation)
 * can be compiled to a command line executable main.cpp 
 * CppTestProject contains an XCode project to debug and test the image processing code
 * is used by the text fairy through a jni wrapper (textfairy/jni)

The following android library projects are included as git submodules:
* [Forked Tesseract Tools for Android by rmtheis] [1]

The following projects where added to the sources directly either because they were modified or they are not available as git repos:
* [hocr2pdf] [2]
* [libjpeg] [3]
* [libpng-android] [4]

  [1]: https://github.com/rmtheis/tess-two
  [2]: http://www.exactcode.com/site/open_source/exactimage/hocr2pdf/
  [3]: http://libjpeg.sourceforge.net/
  [4]: https://github.com/julienr/libpng-android


Building with gradle
--------------------------------------
make sure that you have got the android sdk as well as ndk (r8e) installed

* `git clone git@github.com:renard314/textfairy.git`
* `cd textfairy`
* `git submodule update`
* `git submodule init`
* modify `path` to `ndk-build` in `gradle.properties`
* import `settings.gradle` into android studio or execute `./gradlew build`
