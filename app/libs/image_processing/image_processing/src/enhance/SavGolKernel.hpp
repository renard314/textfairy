//
//  SavGolKernel.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 22/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef SavGolKernel_hpp
#define SavGolKernel_hpp

#include "AlignedArray.h"
#include <vector>
#include <stddef.h>
#include "Point.h"
#include "allheaders.h"


class SavGolKernel
{
public:
    SavGolKernel(Point const& size, Point const& origin,
                 int hor_degree, int vert_degree);
    
    void recalcForOrigin(Point const& origin);
    
    int width() const { return m_width; }
    
    int height() const { return m_height; }
    
    float const* data() const { return m_kernel.data(); }
    
    float operator[](size_t idx) const { return m_kernel[idx]; }
private:
    struct Rotation
    {
        double sin;
        double cos;
        
        Rotation(double s, double c) : sin(s), cos(c) {}
    };
    
    void QR();
    
    /**
     * A matrix of m_numDataPoints rows and m_numVars columns.
     * Stored row by row.
     */
    std::vector<double> m_equations;
    
    /**
     * The data points, in the same order as rows in m_equations.
     */
    std::vector<double> m_dataPoints;
    
    /**
     * The polynomial coefficients of size m_numVars.  Only exists to save
     * one allocation when recalculating the kernel for different data points.
     */
    std::vector<double> m_coeffs;
    
    /**
     * The rotations applied to m_equations as part of QR factorization.
     * Later these same rotations are applied to a copy of m_dataPoints.
     * We could avoid storing rotations and rotate m_dataPoints on the fly,
     * but in that case we would have to rotate m_equations again when
     * recalculating the kernel for different data points.
     */
    std::vector<Rotation> m_rotations;
    
    /**
     * 16-byte aligned convolution kernel of size m_numDataPoints.
     */
    AlignedArray<float, 4> m_kernel;
    
    /**
     * The degree of the polynomial in horizontal direction.
     */
    int m_horDegree;
    
    /**
     * The degree of the polynomial in vertical direction.
     */
    int m_vertDegree;
    
    /**
     * The width of the convolution kernel.
     */
    int m_width;
    
    /**
     * The height of the convolution kernel.
     */
    int m_height;
    
    /**
     * The number of terms in the polynomial.
     */
    int m_numTerms;
    
    /**
     * The number of data points.  This corresponds to the number of items
     * in the convolution kernel.
     */
    int m_numDataPoints;
};

#endif /* SavGolKernel_hpp */
