//
//  plot.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 24/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef plot_hpp
#define plot_hpp

#include "allheaders.h"
#include <string>

void numaPlot(Numa* numa, Numa* numaExtrema, Numa* numaExtrema2, l_int32 outformat, std::string plotName);
l_int32 renderTransformedBoxa(PIX *pixt, BOXA *boxa, l_int32 i);
    
#endif /* plot_hpp */
