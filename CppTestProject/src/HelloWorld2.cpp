//============================================================================
// Name        : HelloWorld2.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <iostream>
#include <allheaders.h>
using namespace std;

int main() {
	Pix* p = pixRead("images/31.jpg");
	pixDisplay(p,0,0);

	pixDestroy(&p);
	return 0;
}
