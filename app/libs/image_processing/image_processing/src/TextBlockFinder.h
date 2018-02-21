/*
 * TextBlockFinder.h
 *
 *  Created on: Jul 13, 2014
 *      Author: renard
 */

#ifndef TEXTBLOCKFINDER_H_
#define TEXTBLOCKFINDER_H_
#include <allheaders.h>

class TextBlockFinder {
public:
	TextBlockFinder(bool debug);

	Pixa* findTextBlocks(Pix* pixb, Pix* pixTextProbability, Numa* naTextSizes,Pix** pixmorphout);
	virtual ~TextBlockFinder();

private:
	void printNuma(Numa* na, const char* tag);

	bool mDebug;
};

#endif /* TEXTBLOCKFINDER_H_ */
