#include "RunningTextlineStats.h"
#include <cmath>
#include <vector>
#include <stdio.h>

RunningTextlineStats::RunningTextlineStats(bool debug) {
	mDebug = debug;
}

void RunningTextlineStats::Push(double x) {
	stats.Push(x);
}

int RunningTextlineStats::Count() const{
	return stats.NumDataValues();
}

void RunningTextlineStats::Clear() {
	stats.Clear();
}

double RunningTextlineStats::Mean() const {
	return stats.Mean();
}

double RunningTextlineStats::Variance() const {
	return stats.Variance();
}

double RunningTextlineStats::StandardDeviation() const {
	return stats.StandardDeviation();
}

double RunningTextlineStats::PopulationStandardDeviation() const {
	return stats.PopulationStandardDeviation();
}

bool RunningTextlineStats::Fits(double lineHeight) const {
	printf("testing: %f",lineHeight);
	if (stats.NumDataValues() == 0) {
		printf(" fits because its the first line\n");
		return true;
	}
	RunningStats nextStats;
	nextStats+=stats;
	nextStats.Push(lineHeight);

	//double error = stats.PopulationStandardDeviation() / stats.Mean();
	double maxError = 0.1;
	double nextError = nextStats.PopulationStandardDeviation() / nextStats.Mean();
	//printf(" next std dev = %f max std dev = %f",nextStddev, maxStddev);
	if(mDebug){
		printf(" next error = %f max error = %f",nextError, maxError);
	}

	//use std dev
	if (nextError> maxError) {
		if(mDebug){
			printf(" no fit\n");
		}
		return false;
	} else {
		if(mDebug){
			printf(" fits!\n");
		}
	}
	return true;
}

