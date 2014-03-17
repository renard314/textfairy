#ifndef RUNNINGTEXTLINESTATS_H
#define RUNNINGTEXTLINESTATS_H

#import "RunningStats.h"

class RunningTextlineStats
{
public:
	RunningTextlineStats();
    void Push(double x);
    bool Fits(double lineHeight) const;
    void Clear();
    int Count() const;
    double Mean() const;
    double Variance() const;
    double StandardDeviation() const;

private:
    RunningStats stats;
};

#endif
