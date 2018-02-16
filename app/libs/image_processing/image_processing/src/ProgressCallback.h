//
//  ProgressCallback.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 04/01/2017.
//  Copyright © 2017 Renard Wellnitz. All rights reserved.
//

#ifndef ProgressCallback_hpp
#define ProgressCallback_hpp

class ProgressCallback {
public:
    virtual void sendMessage(int) = 0;
    virtual void sendPix(Pix*) = 0;
};

#endif /* ProgressCallback_hpp */

