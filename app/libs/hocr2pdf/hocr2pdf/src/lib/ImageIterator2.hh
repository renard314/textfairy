/*
 * Copyright (C) 2008 - 2010 René Rebe, ExactCODE GmbH Germany.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2. A copy of the GNU General
 * Public License can be found in the file LICENSE.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANT-
 * ABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * Alternatively, commercial licensing options are available from the
 * copyright holder ExactCODE GmbH Germany.
 */

#include "Image.hh"

#include <algorithm>


namespace detail 
 { 
     template <typename T> T abs(T const& n) { 
         return (n < (T)0 ? -n : n); 
     } 
} 

class rgb_iterator
{
public:
  uint8_t* ptr;
  uint8_t* ptr_begin;
  const Image& image;
  const int stride;
  
  class accu
  {
  public:
    typedef int32_t vtype;
    static const int samples = 3;
    vtype v[samples];
    
    accu () { v[0] = v[1] = v[2] = 0; }
    
    static accu one () {
      accu a;
      a.v[0] = a.v[1] = a.v[2] = 0xff;
      return a;
    }

    accu& abs() {
      v[0] = std::abs(v[0]);
      v[1] = std::abs(v[1]);
      v[2] = std::abs(v[2]);
      return *this;
    }
    
    void saturate () {
      v[0] = std::min (std::max (v[0], (vtype)0), (vtype)0xff);
      v[1] = std::min (std::max (v[1], (vtype)0), (vtype)0xff);
      v[2] = std::min (std::max (v[2], (vtype)0), (vtype)0xff);
    }
    
    accu& operator*= (vtype f) {
      v[0] *= f;
      v[1] *= f;
      v[2] *= f;
      return *this;
    }

    accu operator* (vtype f) const {
      accu a = *this;
      return a *= f;
    }
    
    accu& operator+= (vtype f) {
      v[0] += f;
      v[1] += f;
      v[2] += f;
      return *this;
    }

    accu operator+ (vtype f) const {
      accu a = *this;
      return a += f;
    }
      
    accu& operator/= (vtype f) {
      v[0] /= f;
      v[1] /= f;
      v[2] /= f;
      return *this;
    }

    accu operator/ (vtype f) const {
      accu a = *this;
      a /= f;
      return a;
    }
    
    accu& operator+= (const accu& other) {
      v[0] += other.v[0];
      v[1] += other.v[1];
      v[2] += other.v[2];
      return *this;
    }
    
    accu operator+ (const accu& other) const {
      accu a = *this;
      a += other;
      return a;
    }

    accu& operator-= (const accu& other) {
      v[0] -= other.v[0];
      v[1] -= other.v[1];
      v[2] -= other.v[2];
      return *this;
    }
    
    accu& operator= (const Image::iterator& background)
    {
      double r = 0, g = 0, b = 0;
      background.getRGB(r, g, b);
      v[0] = (vtype)(r * 0xff);
      v[1] = (vtype)(g * 0xff);
      v[2] = (vtype)(b * 0xff);
      return *this;
    }
    
    void getRGB (vtype& r, vtype& g, vtype& b) {
      r = v[0]; g = v[1]; b = v[2];
    }
    
    void getL (vtype& l) {
      l = (11 * v[0] + 16 * v[1] + 5 * v[2]) / 32;
    }
    
    void setRGB (vtype r, vtype g, vtype b) {
      v[0] = r; v[1] = g; v[2] = b;
    }
  };
    
  rgb_iterator (Image& _image)
    : ptr_begin(_image.getRawData()), image (_image), stride(_image.stride()) {
    ptr = ptr_begin;
  }
    
  rgb_iterator& at (int x, int y) {
    ptr = ptr_begin + y * stride + x * 3;
    return *this;
  }
    
  rgb_iterator& operator++ () {
    ptr += 3;
    return *this;
  }

  rgb_iterator& operator-- () {
    ptr -= 3;
    return *this;
  }
    
  accu operator* () {
    accu a;
    a.v[0] = ptr[0];
    a.v[1] = ptr[1];
    a.v[2] = ptr[2];
    return a;
  }
    
  rgb_iterator& set (const accu& a) {
    ptr[0] = a.v[0];
    ptr[1] = a.v[1];
    ptr[2] = a.v[2];
    return *this;
  }  
};

class rgba_iterator
{
public:
  uint8_t* ptr;
  uint8_t* ptr_begin;
  const Image& image;
  const int stride;
  
  class accu
  {
  public:
    typedef int32_t vtype;
    static const int samples = 4;
    vtype v[samples];
    
    accu () { v[0] = v[1] = v[2] = v[3] = 0; }
    
    static accu one () {
      accu a;
      a.v[0] = a.v[1] = a.v[2] = a.v[3] = 0xff;
      return a;
    }
    
    accu& abs() {
      v[0] = std::abs(v[0]);
      v[1] = std::abs(v[1]);
      v[2] = std::abs(v[2]);
      v[3] = std::abs(v[3]);
      return *this;
    }

    void saturate () {
      v[0] = std::min (std::max (v[0], (vtype)0), (vtype)0xff);
      v[1] = std::min (std::max (v[1], (vtype)0), (vtype)0xff);
      v[2] = std::min (std::max (v[2], (vtype)0), (vtype)0xff);
      v[3] = std::min (std::max (v[3], (vtype)0), (vtype)0xff);
    }
    
    accu& operator*= (vtype f) {
      v[0] *= f;
      v[1] *= f;
      v[2] *= f;
      v[3] *= f;
      return *this;
    }

    accu operator* (vtype f) const {
      accu a = *this;
      return a *= f;
    }
    
    accu& operator+= (vtype f) {
      v[0] += f;
      v[1] += f;
      v[2] += f;
      v[3] += f;
      return *this;
    }
    
    accu operator+ (vtype f) const {
      accu a = *this;
      return a += f;
    }
      
    accu& operator/= (vtype f) {
      v[0] /= f;
      v[1] /= f;
      v[2] /= f;
      v[3] /= f;
      return *this;
    }
    
    accu operator/ (vtype f) const {
      accu a = *this;
      a /= f;
      return a;
    }
    
    accu& operator+= (const accu& other) {
      v[0] += other.v[0];
      v[1] += other.v[1];
      v[2] += other.v[2];
      v[3] += other.v[3];
      return *this;
    }
    
    accu operator+ (const accu& other) const {
      accu a = *this;
      a += other;
      return a;
    }

    accu& operator-= (const accu& other) {
      v[0] -= other.v[0];
      v[1] -= other.v[1];
      v[2] -= other.v[2];
      v[3] -= other.v[3];
      return *this;
    }
    
    accu& operator= (const Image::iterator& background)
    {
      double r = 0, g = 0, b = 0, a = 0;
      background.getRGBA(r, g, b, a);
      v[0] = (vtype)(r * 0xff);
      v[1] = (vtype)(g * 0xff);
      v[2] = (vtype)(b * 0xff);
      v[3] = (vtype)(a * 0xff);
      return *this;
    }
    
    void getRGB (vtype& r, vtype& g, vtype& b) {
      r = v[0]; g = v[1]; b = v[2];
    }
    
    void getL (vtype& l) {
      l = (11 * v[0] + 16 * v[1] + 5 * v[2]) / 32;
    }
    
    void setRGB (vtype r, vtype g, vtype b) {
      v[0] = r; v[1] = g; v[2] = b; v[2] = 0xff;
    }
  };
  
  rgba_iterator (Image& _image)
    : ptr_begin(_image.getRawData()), image (_image), stride(_image.stride()) {
    ptr = ptr_begin;
  }
    
  rgba_iterator& at (int x, int y) {
    ptr = ptr_begin + y * stride + x * 4;
    return *this;
  }
    
  rgba_iterator& operator++ () {
    ptr += 4;
    return *this;
  }

  rgba_iterator& operator-- () {
    ptr -= 4;
    return *this;
  }
  
  accu operator* () {
    accu a;
    a.v[0] = ptr[0];
    a.v[1] = ptr[1];
    a.v[2] = ptr[2];
    a.v[3] = ptr[3];
    return a;
  }
    
  rgba_iterator& set (const accu& a) {
    ptr[0] = a.v[0];
    ptr[1] = a.v[1];
    ptr[2] = a.v[2];
    ptr[3] = a.v[3];
    return *this;
  }
};

class rgb16_iterator
{
public:
  uint16_t* ptr;
  uint16_t* ptr_begin;
  const Image& image;
  const int stride;

    
  class accu
  {
  public:
    typedef int64_t vtype;
    static const int samples = 3;
    vtype v[samples];
    
    accu () { v[0] = v[1] = v[2] = 0; }
    
    static accu one () {
      accu a;
      a.v[0] = a.v[1] = a.v[2] = 0xffff;
      return a;
    }
    
    accu& abs() {
      v[0] = detail::abs(v[0]);
      v[1] = detail::abs(v[1]);
      v[2] = detail::abs(v[2]);
      return *this;
    }

    void saturate () {
      v[0] = std::min (std::max (v[0], (vtype)0), (vtype)0xffff);
      v[1] = std::min (std::max (v[1], (vtype)0), (vtype)0xffff);
      v[2] = std::min (std::max (v[2], (vtype)0), (vtype)0xffff);
    }
    
    accu& operator*= (vtype f) {
      v[0] *= f;
      v[1] *= f;
      v[2] *= f;
      return *this;
    }

    accu operator* (vtype f) const {
      accu a = *this;
      return a *= f;
    }
    
    accu& operator+= (vtype f) {
      v[0] += f;
      v[1] += f;
      v[2] += f;
      return *this;
    }

    accu operator+ (vtype f) const {
      accu a = *this;
      return a += f;
    }
    
    accu& operator/= (vtype f) {
      v[0] /= f;
      v[1] /= f;
      v[2] /= f;
      return *this;
    }
    
    accu operator/ (vtype f) const {
      accu a = *this;
      a /= f;
      return a;
    }
    
    accu& operator+= (const accu& other) {
      v[0] += other.v[0];
      v[1] += other.v[1];
      v[2] += other.v[2];
      return *this;
    }
    
    accu operator+ (const accu& other) const {
      accu a = *this;
      a += other;
      return a;
    }

    accu& operator-= (const accu& other) {
      v[0] -= other.v[0];
      v[1] -= other.v[1];
      v[2] -= other.v[2];
      return *this;
    }
    
    accu& operator= (const Image::iterator& background) {
      double r = 0, g = 0, b = 0;
      background.getRGB(r, g, b);
      v[0] = (vtype)(r * 0xffff);
      v[1] = (vtype)(g * 0xffff);
      v[2] = (vtype)(b * 0xffff);
      return *this;
    }
    
    void getRGB (vtype& r, vtype& g, vtype& b) {
      r = v[0]; g = v[1]; b = v[2];
    }
    
    void getL (vtype& l) {
      l = (11 * v[0] + 16 * v[1] + 5 * v[2]) / 32;
    }
    
    void setRGB (vtype r, vtype g, vtype b) {
      v[0] = r; v[1] = g; v[2] = b;
    }
  };
  
  rgb16_iterator (Image& _image)
    : ptr_begin((uint16_t*)_image.getRawData()), image (_image),
      stride(_image.stride()) {
    ptr = ptr_begin;
  }
  
  rgb16_iterator& at (int x, int y) {
    ptr = ptr_begin + y * stride / 2 + x * 3;
    return *this;
  }
    
  rgb16_iterator& operator++ () {
    ptr += 3;
    return *this;
  }

  rgb16_iterator& operator-- () {
    ptr -= 3;
    return *this;
  }
    
  accu operator* () {
    accu a;
    a.v[0] = ptr[0];
    a.v[1] = ptr[1];
    a.v[2] = ptr[2];
    return a;
  }
    
  rgb16_iterator& set (const accu& a) {
    ptr[0] = a.v[0];
    ptr[1] = a.v[1];
    ptr[2] = a.v[2];
    return *this;
  }
};

class gray_iterator
{
public:
  uint8_t* ptr;
  uint8_t* ptr_begin;
  const Image& image;
  const int stride;
    
  class accu
  {
  public:
    typedef int32_t vtype;
    static const int samples = 1;
    vtype v[samples];
    
    accu () { v[0] = 0; }
    
    static accu one () {
      accu a;
      a.v[0] = 0xff;
      return a;
    }

    accu& abs() {
      v[0] = std::abs(v[0]);
      return *this;
    }

    void saturate () {
      v[0] = std::min (std::max (v[0], (vtype)0), (vtype)0xff);
    }

    accu& operator*= (vtype f) {
      v[0] *= f;
      return *this;
    }

    accu operator* (vtype f) const {
      accu a = *this;
      return a *= f;
    }

    accu& operator+= (vtype f) {
      v[0] += f;
      return *this;
    }

    accu operator+ (vtype f) const {
      accu a = *this;
      return a += f;
    }
      
    accu& operator/= (vtype f) {
      v[0] /= f;
      return *this;
    }

    accu operator/ (vtype f) const {
      accu a = *this;
      a /= f;
      return a;
    }
    
    accu& operator+= (const accu& other) {
      v[0] += other.v[0];
      return *this;
    }

    accu operator+ (const accu& other) const {
      accu a = *this;
      a += other;
      return a;
    }
    
    accu& operator-= (const accu& other) {
      v[0] -= other.v[0];
      return *this;
    }
      
    accu& operator= (const Image::iterator& background) {
      v[0] = background.getL();
      return *this;
    }
    
    void getRGB (vtype& r, vtype& g, vtype& b) {
      r = g = b = v[0];
    }
    
    void getL (vtype& l) {
      l = v[0];
    }
    
    void setRGB (vtype r, vtype g, vtype b) {
      v[0] = (11 * r + 16 * g + 5 * b) / 32;
    }
  };
    
  gray_iterator (Image& _image)
    : ptr_begin(_image.getRawData()), image (_image), stride(_image.stride()) {
    ptr = ptr_begin;
  }
    
  gray_iterator& at (int x, int y) {
    ptr = ptr_begin + y * stride + x;
    return *this;
  }
    
  gray_iterator& operator++ () {
    ptr += 1;
    return *this;
  }

  gray_iterator& operator-- () {
    ptr -= 1;
    return *this;
  }
    
  accu operator* () {
    accu a;
    a.v[0] = ptr[0];
    return a;
  }
    
  gray_iterator& set (const accu& a) {
    ptr[0] = a.v[0];
    return *this;
  }
};

class gray16_iterator
{
public:
  uint16_t* ptr;
  uint16_t* ptr_begin;
  const Image& image;
  const int stride;
  
  class accu
  {
  public:
    typedef int64_t vtype;
    static const int samples = 1;
    vtype v[samples];
    
    accu () { v[0] = 0; }
    
    static accu one () {
      accu a;
      a.v[0] = 0xffff;
      return a;
    }
    
    accu& abs() {
      v[0] = detail::abs(v[0]);
      return *this;
    }

    void saturate () {
      v[0] = std::min (std::max (v[0], (vtype)0), (vtype)0xffff);
    }

    accu& operator*= (vtype f) {
      v[0] *= f;
      return *this;
    }

    accu operator* (vtype f) const {
      accu a = *this;
      return a *= f;
    }
    
    accu& operator+= (vtype f) {
      v[0] += f;
      return *this;
    }

    accu operator+ (vtype f) const {
      accu a = *this;
      return a += f;
    }
    
    accu& operator/= (vtype f) {
      v[0] /= f;
      return *this;
    }
    
    accu operator/ (vtype f) const {
      accu a = *this;
      a /= f;
      return a;
    }
    
    accu& operator+= (const accu& other) {
      v[0] += other.v[0];
      return *this;
    }
    
    accu operator+ (const accu& other) const {
      accu a = *this;
      a += other;
      return a;
    }
    
    accu& operator-= (const accu& other) {
      v[0] -= other.v[0];
      return *this;
    }
    
    accu& operator= (const Image::iterator& background) {
      v[0] = background.getL();
      return *this;
    }
    
    void getRGB (vtype& r, vtype& g, vtype& b) {
      r = g = b = v[0];
    }
    
    void getL (vtype& l) {
      l = v[0];
    }
    
    void setRGB (vtype r, vtype g, vtype b) {
      v[0] = (11 * r + 16 * g + 5 * b) / 32;
    }
    
  };
     
  gray16_iterator (Image& _image)
    : ptr_begin((uint16_t*)_image.getRawData()), image (_image),
      stride(_image.stride()) {
    ptr = ptr_begin;
  }
    
  gray16_iterator& at (int x, int y) {
    ptr = ptr_begin + y * stride/2 + x;
    return *this;
  }
    
  gray16_iterator& operator++ () {
    ptr += 1;
    return *this;
  }

  gray16_iterator& operator-- () {
    ptr -= 1;
    return *this;
  }
    
  accu operator* () {
    accu a;
    a.v[0] = ptr[0];
    return a;
  }
    
  gray16_iterator& set (const accu& a) {
    ptr[0] = a.v[0];
    return *this;
  }
};

template <unsigned int bitdepth>
class bit_iterator
{
public:
  uint8_t* ptr;
  uint8_t* ptr_begin;
  int _x;
  const Image& image;
  const int width, stride;
  int bitpos;
  const int mask;

  typedef gray_iterator::accu accu; // reuse
    
  bit_iterator (Image& _image)
    : ptr_begin(_image.getRawData()), _x(0), image (_image),
      width(_image.width()), stride(_image.stride()),
      bitpos(7), mask ((1 << bitdepth) - 1) {
    ptr = ptr_begin;
  }
    
  bit_iterator& at (int x, int y) {
    _x = x;
    ptr = ptr_begin + y * stride + x / (8 / bitdepth);
    bitpos = 7 - (x % (8 / bitdepth)) * bitdepth;
    return *this;
  }
    
  bit_iterator& operator++ () {
    ++_x;
    bitpos -= bitdepth;
    if (bitpos < 0 || _x == width) {
      if (_x == width)
	_x = 0;
      ++ptr;
      bitpos = 7;
    }
    return *this;
  }

  // untested, TODO: test !!!
  bit_iterator& operator-- () {
    --_x;
    bitpos += bitdepth;
    if (bitpos < 0 || _x < 0) {
      if (_x < 0)
	_x = width;
      --ptr;
      bitpos = 7;
    }
    return *this;
  }
  
  accu operator* () {
    accu a;
    a.v[0] = ((*ptr >> (bitpos - (bitdepth - 1))) & mask) * 0xff / mask;
    return a;
  }
  
  bit_iterator& set (const accu& a) {
    *ptr &= ~(mask << (bitpos - (bitdepth - 1)));
    *ptr |= (a.v[0] >> (8 - bitdepth)) << (bitpos - (bitdepth - 1));
    
    return *this;
  }
};


template <template <typename T> class ALGO, class T1>
void codegen (T1& a1)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1);
  }
}

template <template <typename T> class ALGO, class T1, class T2>
void codegen (T1& a1, T2& a2)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2);
    }
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1, a2);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1, a2);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2);
  }
}

template <template <typename T> class ALGO, class T1, class T2, class T3>
void codegen (T1& a1, T2& a2, T3& a3)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2, a3);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2, a3);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1, a2, a3);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1, a2, a3);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1, a2, a3);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2, a3);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2, a3);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2, a3);
  }
}

template <template <typename T> class ALGO, class T1, class T2, class T3, class T4>
void codegen (T1& a1, T2& a2, T3& a3, T4& a4)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2, a3, a4);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2, a3, a4);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1, a2, a3, a4);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1, a2, a3, a4);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1, a2, a3, a4);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2, a3, a4);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2, a3, a4);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2, a3, a4);
  }
}

template <template <typename T> class ALGO, class T1, class T2, class T3, class T4, class T5>
void codegen (T1& a1, T2& a2, T3& a3, T4& a4, T5& a5)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2, a3, a4, a5);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2, a3, a4, a5);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1, a2, a3, a4, a5);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1, a2, a3, a4, a5);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1, a2, a3, a4, a5);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2, a3, a4, a5);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2, a3, a4, a5);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2, a3, a4, a5);
  }
}

template <template <typename T> class ALGO, class T1, class T2, class T3, class T4, class T5,  class T6>
void codegen (T1& a1, T2& a2, T3& a3, T4& a4, T5& a5, T6& a6)
{
  if (a1.spp == 3) {
    if (a1.spp == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2, a3, a4, a5, a6);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2, a3, a5, a5, a6);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1, a2, a3, a5, a5, a6);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a(a1, a2, a3, a4, a5, a6);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a(a1, a2, a3, a4, a5, a6);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2, a3, a4, a5, a6);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2, a3, a4, a5, a6);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2, a3, a4, a5, a6);
  }
}

template <template <typename T> class ALGO,
	  class T1, class T2, class T3, class T4,
	  class T5, class T6, class T7>
void codegen (T1& a1, T2& a2, T3& a3, T4& a4,
	      T5& a5, T6& a6, T7& a7)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      a (a1, a2, a3, a4, a5, a6, a7);
    } else {
      ALGO <rgb16_iterator> a;
      a (a1, a2, a3, a4, a5, a6, a7);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    a (a1, a2, a3, a5, a5, a6, a7);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    a (a1, a2, a3, a4, a5, a6, a7);
  }
}

// with return

template <class T0, template <typename T> class ALGO,
	  class T1>
T0 codegen_return (T1& a1)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      return a (a1);
    } else {
      ALGO <rgb16_iterator> a;
      return a (a1);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    return a (a1);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    return a (a1);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    return a (a1);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    return a (a1);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    return a (a1);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    return a (a1);
  }
  
  // warn unhandled
  T0 t;
  return t;
}

template <class T0, template <typename T> class ALGO,
	  class T1, class T2>
T0 codegen_return (T1& a1, T2& a2)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      return a (a1, a2);
    } else {
      ALGO <rgb16_iterator> a;
      return a (a1, a2);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    return a (a1, a2);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    return a (a1, a2);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    return a (a1, a2);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    return a (a1, a2);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    return a (a1, a2);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    return a (a1, a2);
  }
  
  // warn unhandled
  T0 t;
  return t;
}

template <class T0, template <typename T> class ALGO,
	  class T1, class T2, class T3>
T0 codegen_return (T1& a1, T2& a2, T3& a3)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      return a (a1, a2, a3);
    } else {
      ALGO <rgb16_iterator> a;
      return a (a1, a2, a3);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    return a (a1, a2, a3);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    return a (a1, a2, a3);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    return a (a1, a2, a3);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    return a (a1, a2, a3);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    return a (a1, a2, a3);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    return a (a1, a2, a3);
  }
  
  // warn unhandled
  T0 t;
  return t;
}

template <class T0, template <typename T> class ALGO,
	  class T1, class T2, class T3, class T4,
	  class T5, class T6, class T7>
T0 codegen_return (T1& a1, T2& a2, T3& a3, T4& a4,
		   T5& a5, T6& a6, T7& a7)
{
  if (a1.spp == 3) {
    if (a1.bps == 8) {
      ALGO <rgb_iterator> a;
      return a (a1, a2, a3, a4, a5, a6, a7);
    } else {
      ALGO <rgb16_iterator> a;
      return a (a1, a2, a3, a4, a5, a6, a7);
    }
  }
  else if (a1.spp == 4 && a1.bps == 8) {
    ALGO <rgba_iterator> a;
    return a (a1, a2, a3, a5, a5, a6, a7);
  }
  else if (a1.bps == 16) {
    ALGO <gray16_iterator> a;
    return a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 8) {
    ALGO <gray_iterator> a;
    return a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 4) {
    ALGO <bit_iterator<4> > a;
    return a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 2) {
    ALGO <bit_iterator<2> > a;
    return a (a1, a2, a3, a4, a5, a6, a7);
  }
  else if (a1.bps == 1) {
    ALGO <bit_iterator<1> > a;
    return a (a1, a2, a3, a4, a5, a6, a7);
  }
  
  // warn unhandled
  T0 t;
  return t;
}
