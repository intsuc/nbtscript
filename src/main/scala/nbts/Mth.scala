package nbts

def floor(value: Float): Int = value.toInt - (if value < value.toInt.toFloat then 1 else 0)

def floor(value: Double): Int = value.toInt - (if value < value.toInt.toDouble then 1 else 0)
