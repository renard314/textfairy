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
* *textfairy* this is the android app
* *hocr2pdf* contains c++ code to create pdf files
 * is used by the text fairy through a jni wrapper (textfairy/jni)
* *OCRTest* contains image processing code (binarization and page segmentation)
 * can be compiled to a command line executable main.cpp to debug/test the image processing code
 * is used by the text fairy through a jni wrapper (textfairy/jni)

The following android library projects are included as git submodules
* [ActionBarSherlock] [1]
* [Android-ViewPagerIndicator] [2]
* [ViewPager3D] [3]

The following projects where added to the sources directly either because they where modified or they are not available as git repos
* [Tesseract (OCR Engine)] [4]
* [Leptonica (Image processing library)] [5]
* [Android Page Curl] [6]
* [hocr2pdf] [7]
* [Tesseract android tools] [8]
* [libjpeg] [9]

  [1]: https://github.com/JakeWharton/ActionBarSherlock
  [2]: https://github.com/JakeWharton/Android-ViewPagerIndicator
  [3]: https://github.com/renard314/ViewPager3D
  [4]: https://tesseract-ocr.googlecode.com/
  [5]: http://www.leptonica.com/index.html
  [6]: https://github.com/harism/android_page_curl/
  [7]: http://www.exactcode.com/site/open_source/exactimage/hocr2pdf/
  [8]: https://code.google.com/p/tesseract-android-tools/
  [9]: http://libjpeg.sourceforge.net/


Building from command line
--------------------------
make sure that you have got the android sdk as well as ndk (r8e) installed

* git clone git@github.com:renard314/textfairy.git
* cd textfairy
* compile native code: ndk-build
* get a coffee
* now create the ant build files for all sub projects
	* android update lib-project -t 17 -p ActionBarSherlock/actionbarsherlock
	* android update lib-project -t 17 -p Android-ViewPagerIndicator/library/
	* android update lib-project -t 17 -p ViewPager3D/
	* android update lib-project -t 17 -p FileExplorer/
* finally create build.xml for root project
	* cd textfairy
	* android update project --path .
* ant-debug

Importing code into eclipse
---------------------------
TODO

