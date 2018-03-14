//
//  ProgressCallback.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 04/01/2017.
//  Copyright © 2017 Renard Wellnitz. All rights reserved.
//

#ifndef ProgressCallback_hpp
#define ProgressCallback_hpp

#include "allheaders.h"

static const int MESSAGE_IMAGE_DETECTION = 0;
static const int MESSAGE_IMAGE_DEWARP = 1;
static const int MESSAGE_OCR = 2;
static const int MESSAGE_ASSEMBLE_PIX = 3;
static const int MESSAGE_ANALYSE_LAYOUT = 4;

class ProgressCallback {
public:
    virtual void sendMessage(int) = 0;
    virtual void sendPix(Pix*) = 0;
};

#endif /* ProgressCallback_hpp */

