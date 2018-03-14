//
//  SavGolFilter.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 22/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "SavGolFilter.hpp"
#include "SavGolKernel.hpp"
#include <stdexcept>
#include <climits>

class Kernel : public SavGolKernel
{
public:
    Kernel(Point const& size, Point const& origin, int hor_degree, int vert_degree)
    : SavGolKernel(size, origin, hor_degree, vert_degree) {}
    
    void convolve(uint8_t* dst, uint8_t const* src_top_left, int src_bpl) const;
};

int bound(int min, int value, int max){
    return std::max(min, std::min(value, max));
}

inline void
Kernel::convolve(uint8_t* dst, uint8_t const* src_top_left, int src_bpl) const
{
    uint8_t const* p_src = src_top_left;
    float const* p_kernel = data();
    float sum = 0.5; // For rounding purposes.
    
    int const w = width();
    int const h = height();
    
    for (int y = 0; y < h; ++y, p_src += src_bpl) {
        for (int x = 0; x < w; ++x) {
            sum += p_src[x] * *p_kernel;
            ++p_kernel;
        }
    }
    
    int const val = static_cast<int>(sum);
    *dst = static_cast<uint8_t>(bound(0, val, 255));
}


int calcNumTerms(int const hor_degree, int const vert_degree){
    return (hor_degree + 1) * (vert_degree + 1);
}


Pix* savGolFilterGrayToGray(Pix* pix, Point window_size, l_int8 hor_degree, l_int8 vert_degree) {
    Pix* src = pixEndianByteSwapNew(pix);
    int const width = pixGetWidth(src);
    int const height = pixGetHeight(src);
    
    // Kernel width and height.
    int const kw = window_size.x;
    int const kh = window_size.y;
    
    if (kw > width || kh > height) {
        return src;
    }
    
    /*
     * Consider a 5x5 kernel:
     * |x|x|T|x|x|
     * |x|x|T|x|x|
     * |L|L|C|R|R|
     * |x|x|B|x|x|
     * |x|x|B|x|x|
     */
    
    // Co-ordinates of the central point (C) of the kernel.
    Point const k_center(kw / 2, kh / 2);
    
    // Origin is the current hot spot of the kernel.
    // Normally it's at k_center, but sometimes we move
    // it to other locations to avoid parts of the kernel
    // from going outside of the image area.
    Point k_origin;
    
    // Length of the top segment (T) of the kernel.
    int const k_top = k_center.y;
    
    // Length of the bottom segment (B) of the kernel.
    int const k_bottom = kh - k_top - 1;
    
    // Length of the left segment (L) of the kernel.
    int const k_left = k_center.x;
    
    // Length of the right segment (R) of the kernel.
    int const k_right = kw - k_left - 1;
    
    uint8_t* src_data = (uint8_t*)pixGetData(src);
    l_int32 wpl = pixGetWpl(src);
//        for (i = 0; i < h; i += factor) {
//            line1 = data1 + i * wpl1;
//            line2 = data2 + i * wpl2;
//            for (j = 0; j < w; j += factor) {
//                val1 = GET_DATA_BYTE(line1, j);
//                val2 = GET_DATA_BYTE(line2, j);
//                val = L_ABS(val1 - val2);
//                array[val]++;
//            }
//        }
    
    int const src_bpl = wpl*4;
    Pix* dst = pixCreate(width, height, 8);
    if (width > 0 && height > 0 && dst==NULL) {
        throw std::bad_alloc();
    }
    
    uint8_t* const dst_data = (uint8_t* const)pixGetData(dst);
    int const dst_bpl =pixGetWpl(dst)*4;
    
    // Top-left corner.
    uint8_t* src_line = src_data;
    uint8_t* dst_line = dst_data;
    Kernel kernel(window_size, Point(0, 0), hor_degree, vert_degree);
    for (int y = 0; y < k_top; ++y, dst_line += dst_bpl) {
        k_origin.y = y;
        for (int x = 0; x < k_left; ++x) {
            k_origin.x= x;
            kernel.recalcForOrigin(k_origin);
            kernel.convolve(dst_line + x, src_line, src_bpl);
        }
    }
    
    // Top area between two corners.
    k_origin.x = k_center.x;
    src_line = src_data - k_left;
    dst_line = dst_data;
    for (int y = 0; y < k_top; ++y, dst_line += dst_bpl) {
        k_origin.y = y;
        kernel.recalcForOrigin(k_origin);
        for (int x = k_left; x < width - k_right; ++x) {
            kernel.convolve(dst_line + x, src_line + x, src_bpl);
        }
    }
    
    // Top-right corner.
    k_origin.y = 0;
    src_line = src_data + width - kw;
    dst_line = dst_data;
    for (int y = 0; y < k_top; ++y, dst_line += dst_bpl) {
        k_origin.x = k_center.x + 1;
        for (int x = width - k_right; x < width; ++x) {
            kernel.recalcForOrigin(k_origin);
            kernel.convolve(dst_line + x, src_line, src_bpl);
            k_origin.x += 1;
        }
        k_origin.y += 1;
    }
    
    // Central area.
    // Take advantage of Savitzky-Golay filter being separable.
    SavGolKernel const hor_kernel(
                                  Point(window_size.x, 1),
                                  Point(k_center.x, 0), hor_degree, 0
                                  );
    SavGolKernel const vert_kernel(
                                   Point(1, window_size.y),
                                   Point(0, k_center.y), 0, vert_degree
                                   );
    
    int const shift = kw - 1;
    
    // Allocate a 16-byte aligned temporary storage.
    // That may help the compiler to emit efficient SSE code.
    int const temp_stride = (width - shift + 3) & ~3;
    AlignedArray<float, 4> temp_array(temp_stride * height);
    
    // Horizontal pass.
    src_line = src_data - shift;
    float* temp_line = temp_array.data() - shift;
    for (int y = 0; y < height; ++y) {
        for (int i = shift; i < width; ++i) {
            float sum = 0.0f;
            
            uint8_t const* src = src_line + i;
            for (int j = 0; j < kw; ++j) {
                sum += src[j] * hor_kernel[j];
            }
            temp_line[i] = sum;
        }
        temp_line += temp_stride;
        src_line += src_bpl;
    }
    
    // Vertical pass.
    dst_line = dst_data + k_top * dst_bpl + k_left - shift;
    temp_line = temp_array.data() - shift;
    for (int y = k_top; y < height - k_bottom; ++y) {
        for (int i = shift; i < width; ++i) {
            float sum = 0.0f;
            
            float* tmp = temp_line + i;
            for (int j = 0; j < kh; ++j, tmp += temp_stride) {
                sum += *tmp * vert_kernel[j];
            }
            int const val = static_cast<int>(sum);
            dst_line[i] = static_cast<uint8_t>(bound(0, val, 255));
        }
        
        temp_line += temp_stride;
        dst_line += dst_bpl;
    }
    
    // Left area between two corners.
    k_origin.x = 0;
    k_origin.y = k_center.y + 1;
    for (int x = 0; x < k_left; ++x) {
        src_line = src_data;
        dst_line = dst_data + dst_bpl * k_top;
        
        kernel.recalcForOrigin(k_origin);
        for (int y = k_top; y < height - k_bottom; ++y) {
            kernel.convolve(dst_line + x, src_line, src_bpl);
            src_line += src_bpl;
            dst_line += dst_bpl;
        }
        k_origin.x += 1;
    }
    
    // Right area between two corners.
    k_origin.x = (k_center.x + 1);
    k_origin.y = (k_center.y);
    for (int x = width - k_right; x < width; ++x) {
        src_line = src_data + width - kw;
        dst_line = dst_data + dst_bpl * k_top;
        
        kernel.recalcForOrigin(k_origin);
        for (int y = k_top; y < height - k_bottom; ++y) {
            kernel.convolve(dst_line + x, src_line, src_bpl);
            src_line += src_bpl;
            dst_line += dst_bpl;
        }
        k_origin.x += 1;
    }
    
    // Bottom-left corner.
    k_origin.y = (k_center.y + 1);
    src_line = src_data + src_bpl * (height - kh);
    dst_line = dst_data + dst_bpl * (height - k_bottom);
    for (int y = height - k_bottom; y < height; ++y, dst_line += dst_bpl) {
        for (int x = 0; x < k_left; ++x) {
            k_origin.x = x;
            kernel.recalcForOrigin(k_origin);
            kernel.convolve(dst_line + x, src_line, src_bpl);
        }
        k_origin.y += 1;
    }
    
    // Bottom area between two corners.
    k_origin.x = (k_center.x);
    k_origin.y = (k_center.y + 1);
    src_line = src_data + src_bpl * (height - kh) - k_left;
    dst_line = dst_data + dst_bpl * (height - k_bottom);
    for (int y = height - k_bottom; y < height; ++y, dst_line += dst_bpl) {
        kernel.recalcForOrigin(k_origin);
        for (int x = k_left; x < width - k_right; ++x) {
            kernel.convolve(dst_line + x, src_line + x, src_bpl);
        }
        k_origin.y += 1;
    }
    
    // Bottom-right corner.
    k_origin.y = (k_center.y + 1);
    src_line = src_data + src_bpl * (height - kh) + (width - kw);
    dst_line = dst_data + dst_bpl * (height - k_bottom);
    for (int y = height - k_bottom; y < height; ++y, dst_line += dst_bpl) {
        k_origin.x = (k_center.x + 1);
        for (int x = width - k_right; x < width; ++x) {
            kernel.recalcForOrigin(k_origin);
            kernel.convolve(dst_line + x, src_line, src_bpl);
            k_origin.x += 1;
        }
        k_origin.y += 1;
    }
    pixEndianByteSwap(dst);
    pixDestroy(&src);
    return dst;
}

Pix* savGolFilter(Pix* src, Point window_size, l_int8 hor_degree, l_int8 vert_degree){
    if (hor_degree < 0 || vert_degree < 0) {
        throw std::invalid_argument("savGolFilter: invalid polynomial degree");
    }
    
    if (calcNumTerms(hor_degree, vert_degree) > window_size.x * window_size.y) {
        throw std::invalid_argument("savGolFilter: order is too big for that window");
    }
    
    if (pixGetDepth(src)!=8) {
        throw std::invalid_argument("savGolFilter: pix is not grayscale");
    }
    
    return savGolFilterGrayToGray(src, window_size, hor_degree, vert_degree);
}

