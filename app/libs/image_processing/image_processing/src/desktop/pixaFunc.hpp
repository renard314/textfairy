//
//  pixaFunc.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 19/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef pixaFunc_hpp
#define pixaFunc_hpp

#include <stdio.h>
#include "allheaders.h"
#include <iostream>
#include <sstream>
#include <fstream>

using namespace std;

typedef void (*PIXA_WRITE_FUNC)(Pixa* pixa, string fileName);

Pix* pixaDisplayDefault(Pixa* pixa);

void scriptDetect(Pixa* pixa, string fileName);
void writePixa(Pixa* pixa, string fileName);
void writeAllPixa(Pixa* pixa, string fileName);
void ignorePixa(Pixa* pixa, string fileName);
void displayPixa(Pixa* pixa, string fileName);
void writeLastPix(Pixa* pixa, string fileName);

#endif /* pixaFunc_hpp */
