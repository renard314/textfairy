/*
 * General purpose encoding and decoding.
 * Copyright (C) 2007 - 2010 Rene Rebe, ExactCODE GmbH
 * Copyright (c) 2008 Valentin Ziegle, ExactCODE GmbH
 * Copyright (C) 2007 Susanne Klaus, ExactCODE GmbH
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

#ifndef ENCODINGS_HH
#define ENCODINGS_HH

#include <inttypes.h>
#include <vector>
#include <iostream>

/* 
   All encoding functions have signature 
   
   void Encode... (std::ostream& stream, const T& data, size_t length ....)

   Type T has to implement const CH& operator[] (int) const
   where CH can be casted into uint8_t. Most likely T will be one out of
   uint8_t*, std::vector<uint8_t>, std::string.
*/

template <typename T>
void EncodeHex(std::ostream& stream, const T& data, size_t length)
{
  const int limit=40;
  static const char nibble[] = "0123456789abcdef";

  for (size_t i = 0; i < length; ++i) {
    if (i % limit == 0 && i != 0)
      stream.put('\n');
    uint8_t byte=data[i];
    stream.put(nibble[byte >> 4]);
    stream.put(nibble[byte & 0x0f]);
  }
}

template <typename T>
void EncodeASCII85(std::ostream& stream, const T& data, size_t length)
{
  const int limit=80;
  int bytes=0;
  if (length>0) {
    length--; // decrementing length allows comparisons below without
              // performing additional arithmetics
    uint32_t quad=0;
    int count=3;
    for (size_t i=0; i<=length; i++) {
      quad <<= 8;
      quad |= (uint8_t) data[i];
      if (i==length || count==0) {
	if (i==length) // pad with zeroes if necessary
	  for (int n=count; n>0; --n)
	    quad <<= 8;

	if (!(count || quad)) {
	  stream.put('z');
	  if(++bytes==limit) {stream.put('\n'); bytes=0;}
	} else {
	  char seq[5];
	  int n;
	  for (n=4; n>=0; n--) {
	    uint32_t rem=quad % 85;
	    quad /= 85;
	    seq[n]='!'+rem;
	  }
	  for (n=0; n<5-count; n++) {
	    stream.put(seq[n]);
	    if(++bytes==limit) {stream.put('\n'); bytes=0;}
	  }
	}
	quad=0;
	count=4;
      }
      count--;
    }
  }
  if (bytes>=limit-1)
    stream.put('\n');
  stream << "~>";
}

template <typename T>
void EncodeBase64(std::ostream& stream, const T& data, size_t length)
{
  const int limit = 57;
  static const char base64lookup[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  for (size_t i = 0; i < length; i += 3) {
    if (i % limit == 0 && i != 0)
      stream.put('\n');
   
    int pad = 0;
    uint32_t v = (uint32_t)data[i + 0] << 16;
    if (i + 1 < length) {
      v |= (uint32_t)data[i + 1] << 8;
      if (i + 2 < length)
	v |= (uint32_t)data[i + 2] << 0;
      else
	pad = 1;
    } else
      pad = 2;
    
    stream.put(base64lookup[v >> 18 & 63]);
    stream.put(base64lookup[v >> 12 & 63]);
    stream.put(pad > 1 ? '=' : base64lookup[(v >> 6 & 63)]);
    stream.put(pad > 0 ? '=' : base64lookup[(v >> 0 & 63)]);
  }
}

template <typename T>
void DecodeBase64(std::ostream& stream, const T& data, size_t length)
{
  static const char base64lookup[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  for (size_t i = 0; i < length; ) {
    uint32_t v = 0;
    int skip = 0;
    
    for (int j = 4; i < length && j > 0;) {
      char c = data[i++];
      // this is a bit sloppy and also skips for '=' in the middle of the
      // stream, but those are not well formed anyway
      if (c == '=') {
	++skip; ++j;
	continue;
      }
      
      // valid base64 content? (skip \n\r et al.)
      unsigned int k;
      for (k = 0; k < sizeof(base64lookup) - 1; ++k)
	if (c == base64lookup[k])
	  break;
      
      if (k != sizeof(base64lookup) - 1) {
	--j;
	//std::cerr << j << ": c: " << c << " to " << k << " for " << std::endl;
	v |= k << (6 * j);
      }
    }
    
    stream.put((char)((v >> 16) & 0xFF));
    if (skip < 2)
      stream.put((char)((v >>  8) & 0xFF));
    if (skip < 1)
      stream.put((char)((v >>  0) & 0xFF));
  }
}

#ifdef WITHZLIB

#include <zlib.h>

// TODO: make template, likewise

// windowBits should be in the rage [8,15]
//            negative values indicate no header and CRC
template <typename T>
bool DecodeZlib(std::ostream& stream,
		const T& data, size_t length,
		const int windowBits = 15)
{
  static const unsigned CHUNK = 16 * 1024;
  
  z_stream z;
  char out[CHUNK];
  
  /* allocate deflate state */
  z.zalloc = Z_NULL;
  z.zfree = Z_NULL;
  z.opaque = Z_NULL;
  int ret = inflateInit2(&z, windowBits);
  if (ret != Z_OK)
    return false;
  
  z.next_in = (Bytef*)&data[0];
  z.avail_in = length;
  
  /* decompress until end of file */
  while (z.avail_in)
  {
    if (false)
    std::cerr << " @ " << z.next_in - (Bytef*)&data[0]
	      << " avail in: " << z.avail_in << std::endl;
      
    /* run inflate() on input until output buffer is full, finish
       decompression if all of source has been read in */
    z.next_out = (Bytef*)out;
    z.avail_out = CHUNK;
    ret = inflate(&z, Z_SYNC_FLUSH);
    if (false)
    std::cerr << "  ret: " << ret
	      << ", avail_out: " << z.avail_out
	      << ", next_in: " << (void*)z.next_in
	      << ", next_out: " << (void*)z.next_out
	      << std::endl;
    
    unsigned have = CHUNK - z.avail_out;
    stream.write(out, have);
    if (!stream) {
      (void)inflateEnd(&z);
      return false;
    }
    if (ret != Z_OK && ret != Z_STREAM_END) {
      (void)inflateEnd(&z);
      return false;
    }
  }

  /* clean up and return */
  (void)deflateEnd(&z);
  return true;  
}

inline bool EncodeZlib (std::ostream& stream, const char* data,
			size_t length, int level = 9)
{
  static const unsigned CHUNK = 16 * 1024;
  
  z_stream z;
  char out[CHUNK];
  
  /* allocate deflate state */
  z.zalloc = Z_NULL;
  z.zfree = Z_NULL;
  z.opaque = Z_NULL;
  int ret = deflateInit(&z, level);
  if (ret != Z_OK)
    return false;
  
  /* compress until end of file */
  z.next_in = (Bytef*)&data[0];
  z.avail_in = length;

  int flush = Z_NO_FLUSH;
  do {
    if (z.avail_in == 0)
	flush = Z_FINISH;

    z.next_out = (Bytef*)out;
    z.avail_out = CHUNK;
    deflate(&z, flush);
    const unsigned have = CHUNK - z.avail_out;

    if (false)
    std::cerr << "  ret: " << ret << ", have: " << have
	      << ", avail_in: " << z.avail_in
	      << ", avail_out: " << z.avail_out
	      << ", next_in: " << (void*)z.next_in
	      << ", next_out: " << (void*)z.next_out
	      << std::endl;

    if (have) stream.write(out, have);
 
    // if there is space in the output buffer start to flush
    if (z.avail_out)
	flush = Z_FINISH;
 
    if (!stream) {
	(void)deflateEnd(&z);
	return false;
    }
  } while (z.avail_out < CHUNK);

  /* clean up and return */
  (void)deflateEnd(&z);
  return true;
}

#endif


/* Some miscellaneous de- and encoders */

inline std::vector<uint32_t>
DecodeUtf8(const char* data, size_t length)
{
  std::vector<uint32_t> store;
  
  for (unsigned int i = 0; i < length;)
    {
      uint32_t g = data[i];
      {
	if (g & (1<<7)) // utf-8 multi-byte-sequence?
	  {
	    // determine byte count
	    int n; // we could pre-shift one bit, or switch on the bits
	    for (n = 0; g & (1<<7); ++n)
	      g <<= 1;
	    if (n < 2 || n > 4) {
	      std::cerr << "invalid utf-8 count: " << n << std::endl;
	    }
	    
	    // first is variable
	    g = ((data[i++]) & (0xff >> n));
	    for (--n; n > 0; --n) {
	      // check the 10 MSB marks for sanity and security
	      if ((data[i] & 0xC0) != 0x80) {
		std::cerr << "incorrect utf-8 multi-byte mark" << std::endl;
	      }
	      g = (g << 6) | ((data[i++]) & (0xff >> 2));
	    }
	  }
	else
	  i++;
	store.push_back(g);
      }
    }
  return store;
}

#endif
