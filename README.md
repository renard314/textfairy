textfairy
=========

Android OCR App

## Building from command line
make sure that you have got the android sdk as well as ndk (r8e) installed

* git clone git@github.com:renard314/textfairy.git
* cd textfairy
* ndk-build
* get a coffee
* now create the ant build files for all sub projects
	* android update lib-project -t 17 -p ActionBarSherlock/actionbarsherlock
	* android update lib-project -t 17 -p Android-ViewPagerIndicator/library/
	* android update lib-project -t 17 -p NineOldAndroids/library/
	* android update lib-project -t 17 -p ViewPager3D/
	* android update lib-project -t 17 -p FileExplorer/
* finally create build.xml for root project
	* cd textfairy
	* android update project --path .
* ant-debug
