//
//  TimerUtil.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 04/10/15.
//  Copyright © 2015 Renard Wellnitz. All rights reserved.
//

#include <string>
#include "allheaders.h"
#include "TimerUtil.h"
#include "image_processing.h"

void printTimer(L_TIMER timer, std::string message) {
    l_float32 time = stopTimerNested(timer);
    if (DEBUG_PERFORMANCE) {
        std::string formattedMessage = message + " = %1.3f \n";
        printf(formattedMessage.c_str(), time);
    }
}
