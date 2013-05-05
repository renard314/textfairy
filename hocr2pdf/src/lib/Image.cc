/*
 * The Plain Old Data encapsulation of pixel, raster data.
 * Copyright (C) 2005 - 2009 René Rebe
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
 
#include <string.h> // memcpy
#include <iostream>

#define DEPRECATED
#include "Image.hh"
#include "../codecs/Codecs.hh"

Image::Image ()
  : modified(false), meta_modified(false), xres(0), yres(0), codec(0), data(0), w(0), h(0), bps(0), spp(0)
{
}

Image::Image (Image& other)
  : modified(false), meta_modified(false), xres(0), yres(0), codec(0), data(0), w(0), h(0), bps(0), spp(0)
{
  operator= (other);
}

Image::~Image () {
  // release attached codec
  if (codec)
    delete (codec); codec = 0;
      
  // release POD
  if (data)
    free (data); data = 0;
}

void Image::copyMeta (const Image& other)
{
  w = other.w;
  h = other.h;
  bps = other.bps;
  spp = other.spp;
  xres = other.xres;
  yres = other.yres;
}

Image& Image::operator= (const Image& other)
{
  copyMeta (other);
  
  uint8_t* d = other.getRawData();
  if (d) {
    resize (w, h);
    memcpy (data, d, stride() * h);
  }
  else {
    setRawData();
  }
  
  return *this;
}

void Image::copyTransferOwnership (Image& other)
{
  copyMeta (other);

  uint8_t* d = other.getRawData();
  other.setRawDataWithoutDelete (0);
  setRawData (d);
}

uint8_t* Image::getRawData () const {
  // ask codec about it
  if (!data && codec) {
    Image* image = const_cast<Image*>(this);
    codec->decodeNow (image);
    if (data) // if data was added
      image->modified = false;
  }
  return data;
}

uint8_t* Image::getRawDataEnd () const {
  // we call getRawData as it might have to query the codec to actually load it
  return getRawData() + h * stride();
}

void Image::setRawData () {
  if (!modified) {
    // DEBUG
    // std::cerr << "Image modified" << std::endl;
    modified = true;
  }
}

void Image::setRawData (uint8_t* _data) {
  if (_data != data && data) {
    free (data);
    data = 0;
  }

  // reuse:
  setRawDataWithoutDelete (_data);
}

void Image::setRawDataWithoutDelete (uint8_t* _data) {
  data = _data;
  
  // reuse
  setRawData ();
}

void Image::resize (int _w, int _h) {
  w = _w;
  h = _h;
  
  // reuse:
  setRawDataWithoutDelete ((uint8_t*) realloc (data, stride() * h));
}

void Image::setDecoderID (const std::string& id) {
  decoderID = id;
}

const std::string& Image::getDecoderID () {
  return decoderID;
}

ImageCodec* Image::getCodec() {
  return codec;
}

void Image::setCodec (ImageCodec* _codec) {
  codec = _codec;
  // at the moment the codec is attached the data recent, 0 on detach
  if (codec) {
    modified = meta_modified = false;
  }
}
