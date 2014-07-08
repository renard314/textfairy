/*
 * TextStatFinder.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef TEXTSTATFINDER_H_
#define TEXTSTATFINDER_H_


#include <allheaders.h>


class TextStatFinder {
public:
	TextStatFinder(Pix* pix);

	void getTextStats(Numa** textSizes);
	virtual ~TextStatFinder();
};

#endif /* TEXTSTATFINDER_H_ */

