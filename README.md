[![Build Status](https://travis-ci.org/renard314/textfairy.svg?branch=master)](https://travis-ci.org/renard314/textfairy)

textfairy
=========

Android OCR App

available in the [play store][1]


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
* *app/libs/hocr2pdf* contains c++ code to create pdf files
* *app/libs/image_processing* contains image processing code (binarization and page segmentation)
  * can be compiled to a command line executable main.cpp 
  * *app/libs/image_processing/CppTestProject* contains an XCode project to debug and test the image processing code on desktop
* *app/libs/[leptonica][6]*, *app/libs/[libjpeg][3]*, *app/libs/[libpng-android][4]*, *app/libs/[hocr2pdf][2]* and *app/libs/[tesseract][5]* are external dependencies that where added to the sources directly either because they were modified or they are not available as git repos.

Building with gradle
--------------------------------------
make sure that you have got the android sdk as well as ndk (r15c) installed

* `git clone git@github.com:renard314/textfairy.git`
* `cd textfairy`
* set `ndk.dir` to point to your `ndk-build` in `gradle.properties`
* `./gradlew app:assembleDevelopDebug`

[1]: https://play.google.com/store/apps/details?id=com.renard.ocr
[2]: http://www.exactcode.com/site/open_source/exactimage/hocr2pdf/
[3]: http://libjpeg.sourceforge.net/
[4]: https://github.com/julienr/libpng-android
[5]: https://github.com/tesseract-ocr/tesseract
[6]: https://github.com/DanBloomberg/leptonica

