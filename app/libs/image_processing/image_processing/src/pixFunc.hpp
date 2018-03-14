//
//  pixFunc.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 19/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef pixFunc_hpp
#define pixFunc_hpp

#include <stdio.h>
#include "allheaders.h"
#include <list>
#include "ProgressCallback.h"
#include <functional>

//typedef Pix* (*PIX_FUNC)(Pix* pix);
typedef std::function<Pix*(Pix* pix)> PIX_FUNC;

Pix* run(Pix* pix, std::list<PIX_FUNC> funcs);
Pix* run(Pix* pix, std::list<PIX_FUNC> funcs, ProgressCallback* callback);

Pix* pixPrepareForOcr(Pix* pix, ProgressCallback* callback);
Pix* pixPrepareLayoutAnalysis(Pix* pix, ProgressCallback* callback);

#ifdef HAS_ADAPTIVE_BINARIZER
Pix* binarizeAdaptive(Pix* pix);
#endif

Pix* binarize(Pix* pix);
Pix* binarizeLegacy(Pix* pix);
Pix* timePixFunc(Pix* pix, PIX_FUNC pixFunc);
Pix* enhance(Pix* pix);
Pix* norm(Pix* pix);
Pix* upscale(Pix* pix);
Pix* convertTo8(Pix* pix);
Pix* medianFilter(Pix* pix);
Pix* unsharpMasking(Pix* pix);
Pix* livreAdapt(Pix* pixs);
Pix* findResolution(Pix* pixs);
Pix* blurDetect(Pix* pixs);
Pix* stats(Pix* pixs);
Pix* savGol(Pix* pix);
Pix* savGol32(Pix* pix);
Pix* reduce2(Pix* pix);
Pix* reduceGray2(Pix* pix);
Pix* reduceGray4(Pix* pix);
Pix* expandGray4(Pix* pix);
Pix* edgeDetect(Pix* pix);
Pix* invert(Pix* pix);
Pix* estimateTextSize(Pix* pix);
Pix* edgeBinarize(Pix* pix);
Pix* deskew(Pix* pix);
Pix* scale2Binary(Pix* pix);
Pix* dewarpOrDeskew(Pix* pix);
Pix* ensure150dpi(Pix* pix);


#endif /* pixFunc_hpp */
