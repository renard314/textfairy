//
//  binarizewolfjolion.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 03/03/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include <iostream>
#include "binarizewolfjolion.hpp"
#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include <opencv2/highgui/highgui.hpp>

using namespace std;
using namespace cv;

#define uget(x,y)    at<unsigned char>(y,x)
#define uset(x,y,v)  at<unsigned char>(y,x)=v;
#define fget(x,y)    at<float>(y,x)
#define fset(x,y,v)  at<float>(y,x)=v;


double calcLocalStats (Mat &im, Mat &map_m, Mat &map_s, int winx, int winy) {
    Mat im_sum, im_sum_sq;
    cv::integral(im,im_sum,im_sum_sq,CV_64F);
    
    double m,s,max_s,sum,sum_sq;
    int wxh	= winx/2;
    int wyh	= winy/2;
    int x_firstth= wxh;
    int y_lastth = im.rows-wyh-1;
    int y_firstth= wyh;
    double winarea = winx*winy;
    
    max_s = 0;
    for	(int j = y_firstth ; j<=y_lastth; j++){
        sum = sum_sq = 0;
        
        // for sum array iterator pointer
        double *sum_top_left = im_sum.ptr<double>(j - wyh);
        double *sum_top_right = sum_top_left + winx;
        double *sum_bottom_left = im_sum.ptr<double>(j - wyh + winy);
        double *sum_bottom_right = sum_bottom_left + winx;
        
        // for sum_sq array iterator pointer
        double *sum_eq_top_left = im_sum_sq.ptr<double>(j - wyh);
        double *sum_eq_top_right = sum_eq_top_left + winx;
        double *sum_eq_bottom_left = im_sum_sq.ptr<double>(j - wyh + winy);
        double *sum_eq_bottom_right = sum_eq_bottom_left + winx;
        
        sum = (*sum_bottom_right + *sum_top_left) - (*sum_top_right + *sum_bottom_left);
        sum_sq = (*sum_eq_bottom_right + *sum_eq_top_left) - (*sum_eq_top_right + *sum_eq_bottom_left);
        
        m  = sum / winarea;
        s  = sqrt ((sum_sq - m*sum)/winarea);
        if (s > max_s) max_s = s;
        
        float *map_m_data = map_m.ptr<float>(j) + x_firstth;
        float *map_s_data = map_s.ptr<float>(j) + x_firstth;
        *map_m_data++ = m;
        *map_s_data++ = s;
        
        // Shift the window, add and remove	new/old values to the histogram
        for	(int i=1 ; i <= im.cols-winx; i++) {
            sum_top_left++, sum_top_right++, sum_bottom_left++, sum_bottom_right++;
            
            sum_eq_top_left++, sum_eq_top_right++, sum_eq_bottom_left++, sum_eq_bottom_right++;
            
            sum = (*sum_bottom_right + *sum_top_left) - (*sum_top_right + *sum_bottom_left);
            sum_sq = (*sum_eq_bottom_right + *sum_eq_top_left) - (*sum_eq_top_right + *sum_eq_bottom_left);
            
            m  = sum / winarea;
            s  = sqrt ((sum_sq - m*sum)/winarea);
            if (s > max_s) max_s = s;
            
            *map_m_data++ = m;
            *map_s_data++ = s;
        }
    }
    
    return max_s;
}



/**********************************************************
 * The binarization routine
 **********************************************************/


void NiblackSauvolaWolfJolion (Mat im, Mat output,
                               int winx, int winy, double k, double dR) {
    
    
    double m, s, max_s;
    double th=0;
    double min_I, max_I;
    int wxh	= winx/2;
    int wyh	= winy/2;
    int x_firstth= wxh;
    int x_lastth = im.cols-wxh-1;
    int y_lastth = im.rows-wyh-1;
    int y_firstth= wyh;
    // int mx, my;
    
    // Create local statistics and store them in a double matrices
    Mat map_m = Mat::zeros (im.rows, im.cols, CV_32F);
    Mat map_s = Mat::zeros (im.rows, im.cols, CV_32F);
    max_s = calcLocalStats (im, map_m, map_s, winx, winy);
    
    minMaxLoc(im, &min_I, &max_I);
    
    Mat thsurf (im.rows, im.cols, CV_32F);
    
    // Create the threshold surface, including border processing
    // ----------------------------------------------------
    for	(int j = y_firstth ; j<=y_lastth; j++) {
        
        float *th_surf_data = thsurf.ptr<float>(j) + wxh;
        float *map_m_data = map_m.ptr<float>(j) + wxh;
        float *map_s_data = map_s.ptr<float>(j) + wxh;
        
        // NORMAL, NON-BORDER AREA IN THE MIDDLE OF THE WINDOW:
        for	(int i=0 ; i <= im.cols-winx; i++) {
            m = *map_m_data++;
            s = *map_s_data++;
            
            th = m + k * (s/max_s-1) * (m-min_I);
            
            // thsurf.fset(i+wxh,j,th);
            *th_surf_data++ = th;
            
            
            if (i==0) {
                // LEFT BORDER
                float *th_surf_ptr = thsurf.ptr<float>(j);
                for (int i=0; i<=x_firstth; ++i)
                    *th_surf_ptr++ = th;
                
                // LEFT-UPPER CORNER
                if (j==y_firstth)
                {
                    for (int u=0; u<y_firstth; ++u)
                    {
                        float *th_surf_ptr = thsurf.ptr<float>(u);
                        for (int i=0; i<=x_firstth; ++i)
                            *th_surf_ptr++ = th;
                    }
                    
                }
                
                // LEFT-LOWER CORNER
                if (j==y_lastth)
                {
                    for (int u=y_lastth+1; u<im.rows; ++u)
                    {
                        float *th_surf_ptr = thsurf.ptr<float>(u);
                        for (int i=0; i<=x_firstth; ++i)
                            *th_surf_ptr++ = th;
                    }
                }
            }
            
            // UPPER BORDER
            if (j==y_firstth)
                for (int u=0; u<y_firstth; ++u)
                    thsurf.fset(i+wxh,u,th);
            
            // LOWER BORDER
            if (j==y_lastth)
                for (int u=y_lastth+1; u<im.rows; ++u)
                    thsurf.fset(i+wxh,u,th);
        }
        
        // RIGHT BORDER
        float *th_surf_ptr = thsurf.ptr<float>(j) + x_lastth;
        for (int i=x_lastth; i<im.cols; ++i)
            // thsurf.fset(i,j,th);
            *th_surf_ptr++ = th;
        
        // RIGHT-UPPER CORNER
        if (j==y_firstth)
        {
            for (int u=0; u<y_firstth; ++u)
            {
                float *th_surf_ptr = thsurf.ptr<float>(u) + x_lastth;
                for (int i=x_lastth; i<im.cols; ++i)
                    *th_surf_ptr++ = th;
            }
        }
        
        // RIGHT-LOWER CORNER
        if (j==y_lastth)
        {
            for (int u=y_lastth+1; u<im.rows; ++u)
            {
                float *th_surf_ptr = thsurf.ptr<float>(u) + x_lastth;
                for (int i=x_lastth; i<im.cols; ++i)
                    *th_surf_ptr++ = th;
            }
        }
    }
    cerr << "surface created" << endl;
    
    for	(int y=0; y<im.rows; ++y)
    {
        unsigned char *im_data = im.ptr<unsigned char>(y);
        float *th_surf_data = thsurf.ptr<float>(y);
        unsigned char *output_data = output.ptr<unsigned char>(y);
        for	(int x=0; x<im.cols; ++x)
        {
            *output_data = *im_data >= *th_surf_data ? 255 : 0;
            im_data++;
            th_surf_data++;
            output_data++;
        }
    }
}

cv::Mat pix8ToMat(Pix *pix8)
{
    cv::Mat mat(cv::Size(pix8->w, pix8->h), CV_8UC1);
    uint32_t *line = pix8->data;
    for (uint32_t y = 0; y < pix8->h; ++y) {
        for (uint32_t x = 0; x < pix8->w; ++x) {
            mat.at<uchar>(y, x) = GET_DATA_BYTE(line, x);
        }
        line += pix8->wpl;
    }
    return mat;
}

Pix *mat8ToPix(cv::Mat *mat8)
{
    Pix *pixd = pixCreate(mat8->size().width, mat8->size().height, 1);
    for(int y=0; y<mat8->rows; y++) {
        for(int x=0; x<mat8->cols; x++) {
            pixSetPixel(pixd, x, y, (l_uint32) 255-mat8->at<uchar>(y,x));
        }
    }
    return pixd;
}

Pix* binarizeWolf(Pix* pix){
    Mat input = pix8ToMat(pix);
    
    if ((input.rows<=0) || (input.cols<=0)) {
        cerr << "*** ERROR: Couldn't read input image " << endl;
        exit(1);
    }
    
    // Treat the window size
    cerr << "Input size: " << input.cols << "x" << input.rows << endl;
    int winy = (int) (2.0 * input.rows-1)/3;
    int winx = (int) input.cols-1 < winy ? input.cols-1 : winy;
    // if the window is too big, than we asume that the image
    // is not a single text box, but a document page: set
    // the window size to a fixed constant.
    if (winx > 100) {
        winx = winy = 30;
    }
    cout << "Setting window size to [" << winx << "," << winy << "].\n";
    
    // Threshold
    Mat output (input.rows, input.cols, CV_8U);
    NiblackSauvolaWolfJolion (input, output, winx, winy, 0.2, 128);
    
    Pix* result = mat8ToPix(&output);
    
    l_int32 xRes, yRes;
    pixGetResolution(pix, &xRes, &yRes);
    pixSetResolution(result, xRes, yRes);
    
    return result;

    
}
