package scalaxy; package components

// This will hopefully not exist anymore when 2.10.0.final is out!
object HacksAndWorkarounds
{
  final val debugFailedMatches = false
  final val onlyTryPatternsWithSameClass = false
  
  // TODO turn to false once macro type is fixed !
  final val workAroundMissingTypeApply = true
  final val workAroundNullPatternTypes = true

  final val fixTypedExpressionsType = true
  final val healSymbols = true

  final val useStringBasedTypeEqualityInBindings = true
  final val useStringBasedPatternMatching = false
}
