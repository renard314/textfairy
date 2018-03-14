//
//  Point.h
//  ImageProcessing
//
//  Created by Renard Wellnitz on 22/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef Point_h
#define Point_h

class Point {
public:
    Point():x(0),y(0){
        
    }
    Point(int _x, int _y):x(_x),y(_y){
                
    }
    int x;
    int y;
};

#endif /* Point_h */
