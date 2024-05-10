angleGreaterThan180(Angle):-Angle > 180.
angleLessThanMinus180(Angle):-Angle < -180.
timeModulo20(Time):-Time mod 20 =:= 0.
velocityIsZero(Velocity):-Velocity =:= 0.
numberOfHitsIsOne(NumberOfHits):-NumberOfHits =:= 1.
hitWallCountAndTimeCheck(HitWallCount, Time, FirstHitWallTime, HitNumbersToSwitchMovement, HitWallEvaluateInterval):-HitWallCount >= HitNumbersToSwitchMovement,Time - FirstHitWallTime =< HitWallEvaluateInterval.
evaluateHitInterval(Time, FirstHitTime, HitEvaluateInterval):-Time - FirstHitTime > HitEvaluateInterval.