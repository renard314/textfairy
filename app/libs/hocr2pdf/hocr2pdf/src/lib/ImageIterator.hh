#ifdef CONST
#define iterator const_iterator
#else
#define CONST
#endif

  class iterator
  {
  public:
    CONST Image* image;
    
    type_t type;
    /* TODO: should be unsigned */
    int stride, width, _x;
    ivalue_t value;

    value_t* ptr;
    signed int bitpos; // for 1bps sub-position
    
    // for seperate use, e.g. to accumulate
    iterator ()
    {};
    
    iterator (CONST Image* _image, bool end)
      : image (_image), type (_image->Type()),
	stride (_image->stride()), width (image->w)
    {
      if (!end) {
	ptr = (value_t*) image->getRawData();
	_x = 0;
	bitpos = 7;
      }
      else {
	ptr = (value_t*) image->getRawDataEnd();
	_x = width;
	// TODO: bitpos= ...
      }
    }

    value_t* end_ptr() const
    {
        return (value_t*) image->getRawDataEnd();
    }

    inline void clear () {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray = 0;
	break;
      case RGB8A:
	value.rgba.a=0;
      case RGB8:
      case RGB16:
	value.rgb.r = value.rgb.g = value.rgb.b = 0;
	break;
      case CMYK8:
	value.cmyk.c = value.cmyk.m = value.cmyk.y = value.cmyk.k = 0;
	break;
      case YUV8:
	value.yuv.y = value.yuv.u = value.yuv.v = 0;
	break;
      default:
	WARN_UNHANDLED;
      }
    }
    
    inline iterator at (int x, int y) {
      iterator tmp = *this;
      
      switch (type) {
      case GRAY1:
	tmp.ptr = (value_t*) (image->data + stride * y + x / 8);
	tmp.bitpos = 7 - x % 8;
	tmp._x = x;
	break;
      case GRAY2:
	tmp.ptr = (value_t*) (image->data + stride * y + x / 4);
	tmp.bitpos = 7 - (x % 4) * 2;
	tmp._x = x;
	break;
      case GRAY4:
	tmp.ptr = (value_t*) (image->data + stride * y + x / 2);
	tmp.bitpos = 7 - (x % 2) * 4;
	tmp._x = x;
	break;
      case GRAY8:
	tmp.ptr = (value_t*) (image->data + stride * y + x);
	break;
      case GRAY16:
	tmp.ptr = (value_t*) (image->data + stride * y + x * 2);
	break;
      case RGB8:
      case YUV8:
	tmp.ptr = (value_t*) (image->data + stride * y + x * 3);
	break;
      case RGB8A:
	tmp.ptr = (value_t*) (image->data + stride * y + x * 4);
	break;
      case RGB16:
	tmp.ptr = (value_t*) (image->data + stride * y + x * 6);
	break;
      case CMYK8:
	tmp.ptr = (value_t*) (image->data + stride * y + x * 4);
	break;
      default:
	WARN_UNHANDLED;
      }
      return tmp;
    }

    inline iterator& operator* () {
      switch (type) {
      case GRAY1:
	value.gray = (ptr->gray >> (bitpos-0) & 0x01) * 255;
	break;
      case GRAY2:
	value.gray = (ptr->gray >> (bitpos-1) & 0x03) * 255/3;
	break;
      case GRAY4:
	value.gray = (ptr->gray >> (bitpos-3) & 0x0f) * 255/15;
	break;
      case GRAY8:
	value.gray = ptr->gray;
	break;
      case GRAY16:
	value.gray = ptr->gray16;
	break;
      case RGB8:
	value.rgb.r = ptr->rgb.r;
	value.rgb.g = ptr->rgb.g;
	value.rgb.b = ptr->rgb.b;
	break;
      case RGB8A:
	value.rgba.r = ptr->rgba.r;
	value.rgba.g = ptr->rgba.g;
	value.rgba.b = ptr->rgba.b;
	value.rgba.a = ptr->rgba.a;
	break;
      case RGB16:
	value.rgb.r = ptr->rgb16.r;
	value.rgb.g = ptr->rgb16.g;
	value.rgb.b = ptr->rgb16.b;
	break;
      case CMYK8:
	value.cmyk.c = ptr->cmyk.c;
	value.cmyk.m = ptr->cmyk.m;
	value.cmyk.y = ptr->cmyk.y;
	value.cmyk.k = ptr->cmyk.k;
	break;
      case YUV8:
	value.yuv.y = ptr->yuv.y;
	value.yuv.u = ptr->yuv.u;
	value.yuv.v = ptr->yuv.v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator& operator+= (const iterator& other) {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray += other.value.gray;
	break;
      case RGB8:
      case RGB16:
	value.rgb.r += other.value.rgb.r;
	value.rgb.g += other.value.rgb.g;
	value.rgb.b += other.value.rgb.b;
	break;
      case RGB8A:
	value.rgba.r += other.value.rgba.r;
	value.rgba.g += other.value.rgba.g;
	value.rgba.b += other.value.rgba.b;
	value.rgba.a += other.value.rgba.a;
	break;
      case CMYK8:
	value.cmyk.c += other.value.cmyk.c;
	value.cmyk.m += other.value.cmyk.m;
	value.cmyk.y += other.value.cmyk.y;
	value.cmyk.k += other.value.cmyk.k;
	break;
      case YUV8:
	value.yuv.y += other.value.yuv.y;
	value.yuv.u += other.value.yuv.u;
	value.yuv.v += other.value.yuv.v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator operator+ (const iterator& other) const {
      iterator tmp = *this;
      return tmp += other;
    }
    
    inline iterator operator+ (int v) const {
      iterator tmp = *this;
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	tmp.value.gray += v;
	break;
      case RGB8:
      case RGB16:
	tmp.value.rgb.r += v;
	tmp.value.rgb.g += v;
	tmp.value.rgb.b += v;
	break;
      case RGB8A:
	tmp.value.rgba.r += v;
	tmp.value.rgba.g += v;
	tmp.value.rgba.b += v;
	tmp.value.rgba.a += v;
	break;
      case CMYK8:
	tmp.value.cmyk.c += v;
	tmp.value.cmyk.m += v;
	tmp.value.cmyk.y += v;
	tmp.value.cmyk.k += v;
	break;
      case YUV8:
	tmp.value.yuv.y += v;
	tmp.value.yuv.u += v;
	tmp.value.yuv.v += v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return tmp;
    }
    
    inline iterator& operator-= (const iterator& other)  {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray -= other.value.gray;
	break;
      case RGB8:
      case RGB16:
	value.rgb.r -= other.value.rgb.r;
	value.rgb.g -= other.value.rgb.g;
	value.rgb.b -= other.value.rgb.b;
	break;
      case RGB8A:
	value.rgba.r -= other.value.rgba.r;
	value.rgba.g -= other.value.rgba.g;
	value.rgba.b -= other.value.rgba.b;
	value.rgba.a -= other.value.rgba.a;
	break;
      case CMYK8:
	value.cmyk.c -= other.value.cmyk.c;
	value.cmyk.m -= other.value.cmyk.m;
	value.cmyk.y -= other.value.cmyk.y;
	value.cmyk.k -= other.value.cmyk.k;
	break;
      case YUV8:
	value.yuv.y -= other.value.yuv.y;
	value.yuv.u -= other.value.yuv.u;
	value.yuv.v -= other.value.yuv.v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator& operator- (const iterator& other) const {
      iterator tmp = *this;
      return tmp -= other;
    }
    
    inline iterator& operator*= (const int v) {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray *= v;
	break;
      case RGB8:
      case RGB16:
	value.rgb.r *= v;
	value.rgb.g *= v;
	value.rgb.b *= v;
	break;
      case RGB8A:
	value.rgba.r *= v;
	value.rgba.g *= v;
	value.rgba.b *= v;
	value.rgba.a *= v;
	break;
      case CMYK8:
	value.cmyk.c *= v;
	value.cmyk.m *= v;
	value.cmyk.y *= v;
	value.cmyk.k *= v;
	break;
      case YUV8:
	value.yuv.y *= v;
	value.yuv.u *= v;
	value.yuv.v *= v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator operator* (const int v) const {
      iterator tmp = *this;
      return tmp *= v;
    }
    
    inline iterator& operator/= (const int v) {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray /= v;
	break;
      case RGB8:
      case RGB16:
	value.rgb.r /= v;
	value.rgb.g /= v;
	value.rgb.b /= v;
	break;
      case RGB8A:
	value.rgba.r /= v;
	value.rgba.g /= v;
	value.rgba.b /= v;
	value.rgba.a /= v;
	break;
      case CMYK8:
	value.cmyk.c /= v;
	value.cmyk.m /= v;
	value.cmyk.y /= v;
	value.cmyk.k /= v;
	break;
      case YUV8:
	value.yuv.y /= v;
	value.yuv.u /= v;
	value.yuv.v /= v;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }

    inline iterator& operator/ (const int v) const {
      iterator tmp = *this;
      return tmp /= v;
    }
    
    inline iterator& limit () {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	if (value.gray > 0xff)
	  value.gray = 0xff;
	break;
      case RGB8:
	if (value.rgb.r > 0xff)
	  value.rgb.r = 0xff;
	if (value.rgb.g > 0xff)
	  value.rgb.g = 0xff;
	if (value.rgb.b > 0xff)
	  value.rgb.b = 0xff;
	break;
      case RGB8A:
	if (value.rgba.r > 0xff)
	  value.rgba.r = 0xff;
	if (value.rgba.g > 0xff)
	  value.rgb.g = 0xff;
	if (value.rgba.b > 0xff)
	  value.rgba.b = 0xff;
	if (value.rgba.a > 0xff)
	  value.rgba.a = 0xff;
	break;
      case RGB16:
	if (value.rgb.r > 0xffff)
	  value.rgb.r = 0xffff;
	if (value.rgb.g > 0xffff)
	  value.rgb.g = 0xffff;
	if (value.rgb.b > 0xffff)
	  value.rgb.b = 0xffff;
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    //prefix
    inline iterator& operator++ () {
      switch (type) {
      case GRAY1:
	--bitpos; ++_x;
	if (bitpos < 0 || _x == width) {
	  bitpos = 7;
	  if (_x == width)
	    _x = 0;
	  ptr = (value_t*) ((uint8_t*) ptr + 1);
	}
	break;
      case GRAY2:
	bitpos -= 2; ++_x;
	if (bitpos < 0 || _x == width) {
	  bitpos = 7;
	  if (_x == width)
	    _x = 0;
	  ptr = (value_t*) ((uint8_t*) ptr + 1);
	}
	break;
      case GRAY4:
	bitpos -= 4; ++_x;
	if (bitpos < 0 || _x == width) {
	  bitpos = 7;
	  if (_x == width)
	    _x = 0;
	  ptr = (value_t*) ((uint8_t*) ptr + 1);
	}
	break;
      case GRAY8:
	ptr = (value_t*) ((uint8_t*) ptr + 1);
	break;
      case GRAY16:
	ptr = (value_t*) ((uint8_t*) ptr + 2); break;
      case RGB8:
      case YUV8:
	ptr = (value_t*) ((uint8_t*) ptr + 3); break;
     case RGB8A:
	ptr = (value_t*) ((uint8_t*) ptr + 4); break;
      case RGB16:
	ptr = (value_t*) ((uint8_t*) ptr + 6); break;
      case CMYK8:
	ptr = (value_t*) ((uint8_t*) ptr + 4); break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator& down() {
      switch (type) {
      case GRAY1:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            ptr = (value_t*) ((uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)));
            --bitpos; ++_x;
            if (bitpos < 0) {
                bitpos = 7;
                ptr = (value_t*) ((uint8_t*) ptr + 1);
            } else if (_x == width) {
                ptr = end_ptr();
            }
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case GRAY2:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            ptr = (value_t*) ((uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)));
            bitpos -= 2; ++_x;
            if (bitpos < 0) {
                bitpos = 7;
                ptr = (value_t*) ((uint8_t*) ptr + 1);
            } else if (_x == width) {
                ptr = end_ptr();
            }
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case GRAY4:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            ptr = (value_t*) ((uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)));
            bitpos -= 4; ++_x;
            if (bitpos < 0) {
                bitpos = 7;
                ptr = (value_t*) ((uint8_t*) ptr + 1);
            } else if (_x == width) {
                ptr = end_ptr();
            }
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case GRAY8:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 1 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 1);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case GRAY16:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 2 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 2);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case RGB8:
      case YUV8:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 3 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 3);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case RGB8A:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 4 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 4);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case RGB16:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 6 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 6);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      case CMYK8:
        if ( (uint8_t*) ptr + stride >= (uint8_t*) end_ptr() ) {
            if ( (uint8_t*) ptr + 4 >= (uint8_t*) end_ptr() ) ptr = end_ptr();
            else ptr = (value_t*) ( (uint8_t*) image->data + (stride - ((uint8_t*) end_ptr() - (uint8_t*) ptr)) + 4);
        } else {
            ptr = (value_t*) ((uint8_t*) ptr + stride);
        }
	break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    inline iterator& operator-- () {
      switch (type) {
      case GRAY1:
	++bitpos; --_x;
	if (bitpos > 7) {
	  bitpos = 0;
	  ptr = (value_t*) ((uint8_t*) ptr - 1);
	}
	break;
      case GRAY2:
	bitpos += 2; --_x;
	if (bitpos > 7) {
	  bitpos = 1;
	  ptr = (value_t*) ((uint8_t*) ptr - 1);
	}
	break;
      case GRAY4:
	bitpos += 4; --_x;
	if (bitpos > 7) {
	  bitpos = 3;
	  ptr = (value_t*) ((uint8_t*) ptr - 1);
	}
	break;
      case GRAY8:
	ptr = (value_t*) ((uint8_t*) ptr - 1); break;
      case GRAY16:
	ptr = (value_t*) ((uint8_t*) ptr - 2); break;
      case RGB8:
      case YUV8:
	ptr = (value_t*) ((uint8_t*) ptr - 3); break;
      case RGB8A:
	ptr = (value_t*) ((uint8_t*) ptr - 4); break;
      case RGB16:
	ptr = (value_t*) ((uint8_t*) ptr - 6); break;
      case CMYK8:
	ptr = (value_t*) ((uint8_t*) ptr - 4); break;
      default:
	WARN_UNHANDLED;
      }
      return *this;
    }
    
    // return Luminance
    inline uint16_t getL () const
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	return value.gray;
      case RGB8A: // Todo: check that
	return (uint16_t) (.21267 * value.rgba.r +
			   .71516 * value.rgba.g +
			   .07217 * value.rgba.b);
      case RGB8:
      case RGB16:
	return (uint16_t) (.21267 * value.rgb.r +
			   .71516 * value.rgb.g +
			   .07217 * value.rgb.b);
      case CMYK8:
	return value.cmyk.k; // TODO
      case YUV8:
	return value.yuv.y;
      default:
	WARN_UNHANDLED;
	return 0;
      }
    }
    
    // return RGB
    inline void getRGB(uint16_t* r, uint16_t* g, uint16_t* b) const
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	*r = *g = *b = value.gray;
	break;
      case RGB8:
      case RGB16:
	*r = value.rgb.r;
	*g = value.rgb.g;
	*b = value.rgb.b;
	break;
      case RGB8A:
	*r = value.rgba.r;
	*g = value.rgba.g;
	*b = value.rgba.b;
	break;
      default:
	WARN_UNHANDLED;
      }
    }

    // return RGB
    inline void getRGB(double& r, double& g, double& b) const
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
	r = g = b = (double)value.gray / 0xff; break;
      case GRAY16:
	r = g = b = (double)value.gray / 0xffff; break;
      case RGB8:
	r = (double)value.rgb.r / 0xff;
	g = (double)value.rgb.g / 0xff;
	b = (double)value.rgb.b / 0xff;
	break;
      case RGB8A:
	r = (double)value.rgba.r / 0xff;
	g = (double)value.rgba.g / 0xff;
	b = (double)value.rgba.b / 0xff;
	break;
      case RGB16:
	r = (double)value.rgb.r / 0xffff;
	g = (double)value.rgb.g / 0xffff;
	b = (double)value.rgb.b / 0xffff;
	break;
      default:
	WARN_UNHANDLED;
      }
    }
 

   // return RGBA
    inline void getRGBA(uint16_t* r, uint16_t* g, uint16_t* b, uint16_t* a) const
    {
      getRGB(r,g,b);
      switch (type) {
      case GRAY16:
      case RGB16:
	*a=0xffff;
	break;
      case RGB8A:
	*a=value.rgba.a;
	break;
      default:
	*a=0xff;
      }
    }

    // return RGB
    inline void getRGBA(double& r, double& g, double& b, double& a) const
    {
      getRGB(r,g,b);
      a=(type==RGB8A)? ((double)value.rgba.a / 0xff): 1.0;
    }
 
    inline void setL (uint16_t L)
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray = L;
	break;
      case RGB8:
      case RGB16:
	value.rgb.r = value.rgb.g = value.rgb.b = L;
	break;
      case RGB8A: // Todo: check that
	value.rgba.r = value.rgba.g = value.rgba.b = L;
	break;
      case CMYK8:
	// TODO:
	value.cmyk.c = value.cmyk.m = value.cmyk.y = 0;
	value.cmyk.k = L;
	break;
      case YUV8:
	value.yuv.u = value.yuv.v = 0;
	value.yuv.y = L;
 	break;
      }
    }
    
    // set RGB
    inline void setRGB(uint16_t r, uint16_t g, uint16_t b)
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
      case GRAY16:
	value.gray = (int) (.21267 * r + .71516 * g + .07217 * b);
	break;
      case RGB8:
      case RGB16:
	value.rgb.r = r;
	value.rgb.g = g;
	value.rgb.b = b;
	break;
      case RGB8A: // Todo: check that
	value.rgba.r = r;
	value.rgba.g = g;
	value.rgba.b = b;
	break;
      default:
	WARN_UNHANDLED;
      }
    }

    // set RGB
    inline void setRGB(double r, double g, double b)
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
      case GRAY8:
	value.gray = (int) ((.21267 * r + .71516 * g + .07217 * b) * 0xff);
	break;
      case GRAY16:
	value.gray = (int) ((.21267 * r + .71516 * g + .07217 * b) * 0xffff);
	break;
      case RGB8:
	value.rgb.r = (int) (r * 0xff);
	value.rgb.g = (int) (g * 0xff);
	value.rgb.b = (int) (b * 0xff);
	break;
      case RGB8A: // Todo: check that
	value.rgba.r = (int) (r * 0xff);
	value.rgba.g = (int) (g * 0xff);
	value.rgba.b = (int) (b * 0xff);
	break;
      case RGB16:
	value.rgb.r = (int) (r * 0xffff);
	value.rgb.g = (int) (g * 0xffff);
	value.rgb.b = (int) (b * 0xffff);
	break;
      default:
	WARN_UNHANDLED;
      }
    }

    // set RGBA
    inline void setRGBA(uint16_t r, uint16_t g, uint16_t b, uint16_t a)
    {
      setRGB(r,g,b);
      if (type==RGB8A)
	value.rgba.a=a;
    }

    // set RGBA
    inline void setRGBA(double r, double g, double b, double a)
    {
      setRGB(r,g,b);
      if (type==RGB8A)
	value.rgba.a=(int) (a * 0xff);
    }

    inline void set (const iterator& other) {
      switch (type) {
      case GRAY1:
	ptr->gray = (ptr->gray & (~(1<<bitpos))) | (other.value.gray >> 7) << bitpos;
	break;
      case GRAY2:
	ptr->gray = (ptr->gray & (~(3<<(bitpos-1)))) | (other.value.gray >> 6) << (bitpos-1);
	break;
      case GRAY4:
	ptr->gray = (ptr->gray & (~(15<<(bitpos-3)))) | (other.value.gray >> 4) << (bitpos-3);
	break;
      case GRAY8:
	ptr->gray = other.value.gray;
	break;
      case GRAY16:
	ptr->gray16 = other.value.gray;
	break;
      case RGB8:
	ptr->rgb.r = other.value.rgb.r;
	ptr->rgb.g = other.value.rgb.g;
	ptr->rgb.b = other.value.rgb.b;
	break;
      case RGB8A:
	ptr->rgba.r = other.value.rgba.r;
	ptr->rgba.g = other.value.rgba.g;
	ptr->rgba.b = other.value.rgba.b;
	ptr->rgba.a = other.value.rgba.a;
	break;
      case RGB16:
	ptr->rgb16.r = other.value.rgb.r;
	ptr->rgb16.g = other.value.rgb.g;
	ptr->rgb16.b = other.value.rgb.b;
	break;
      case CMYK8:
	ptr->cmyk.c = other.value.cmyk.c;
	ptr->cmyk.m = other.value.cmyk.m;
	ptr->cmyk.y = other.value.cmyk.y;
	ptr->cmyk.k = other.value.cmyk.k;
	break;
      case YUV8:
	ptr->yuv.y = other.value.yuv.y;
	ptr->yuv.u = other.value.yuv.u;
	ptr->yuv.v = other.value.yuv.v;
	break;
      default:
	WARN_UNHANDLED;
      }
    }
    
    bool operator != (const iterator& other) const
    {
      switch (type) {
      case GRAY1:
      case GRAY2:
      case GRAY4:
	return ptr != other.ptr && _x != other._x;
      default:
	return ptr != other.ptr;
      }

    }

#ifndef iterator

    operator const_iterator ()
    {
	const_iterator it (image, false);
        it._x = _x;
        it.bitpos = bitpos;
        it.ptr = ptr;
	return it;
    }

#endif

  };

#undef iterator
#undef CONST
