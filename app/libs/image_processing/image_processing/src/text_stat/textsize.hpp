//
//  textsize.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 24/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef textsize_hpp
#define textsize_hpp

#include "allheaders.h"

l_float32 pixGetTextLineHeightGeneral(Pix* pix, l_int32* pLineCount);
l_float32 pixGetTextLineHeight(Pix* pixb, l_int32 numTilesY, l_int32 numTilesX, l_int32 minLineCount, l_int32* pLineCount);

#endif /* textsize_hpp */
