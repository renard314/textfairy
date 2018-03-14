//
//  ocrtest.h
//  ImageProcessing
//
//  Created by Renard Wellnitz on 19/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef ocrtest_h
#define ocrtest_h

#include "allheaders.h"
#include <string>

int ocr(Pix* pix, std::string lang);
std::string detectLanguage(Pix* pix, l_float32* pConf);
Boxa* analyseLayout(Pix* pix, std::string lang);

#endif /* ocrtest_h */
