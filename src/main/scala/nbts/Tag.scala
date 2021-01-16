package nbts

import scala.collection.mutable

sealed trait Tag:
  def copy(): Tag = this

object EndTag extends Tag

final case class StringTag private (data: String) extends Tag:
  def size: Int = data.size

object StringTag:
  val Empty = new StringTag("")
  def apply(data: String): StringTag = if data.isEmpty then Empty else new StringTag(data)

final case class CompoundTag(private val data: mutable.Map[String, Tag] = mutable.HashMap.empty) extends Tag with mutable.Map[String, Tag]:
  override def copy(): CompoundTag = CompoundTag(mutable.HashMap.empty ++ data.view.mapValues(_.copy()))

  def addOne(element: (String, Tag)) =
    data.addOne(element); this

  def get(key: String): Option[Tag] = data.get(key)

  def iterator: Iterator[(String, Tag)] = data.iterator

  def subtractOne(element: String) =
    data.subtractOne(element); this

sealed trait NumericTag extends Tag:
  def asByte: Byte
  def asShort: Short
  def asInt: Int
  def asLong: Long
  def asFloat: Float
  def asDouble: Double
  def asNumber: Number

final case class ByteTag private (data: Byte) extends NumericTag:
  def asByte: Byte = data
  def asShort: Short = data
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object ByteTag:
  inline val MinCache = -128
  inline val MaxCache = 127
  private val Cache: Seq[ByteTag] = (MinCache to MaxCache).map(data => new ByteTag(data.toByte))
  val Zero: ByteTag = ByteTag(0.toByte)
  val One: ByteTag = ByteTag(1.toByte)
  def apply(data: Byte): ByteTag = Cache(data + 128)
  def apply(data: Boolean): ByteTag = if data then One else Zero

final case class ShortTag private (data: Short) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = data
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object ShortTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[ShortTag] = (MinCache to MaxCache).map(data => new ShortTag(data.toShort))
  def apply(data: Short): ShortTag = Cache.lift(data - MinCache).getOrElse(new ShortTag(data))

final case class IntTag private (data: Int) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = (data & 0xffff).toShort
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data.toFloat
  def asDouble: Double = data
  def asNumber: Number = data

object IntTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[IntTag] = (MinCache to MaxCache).map(data => new IntTag(data))
  def apply(data: Int): IntTag = Cache.lift(data - MinCache).getOrElse(new IntTag(data))

final case class LongTag private (data: Long) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = (data & 0xffff).toShort
  def asInt: Int = data.toInt
  def asLong: Long = data
  def asFloat: Float = data.toFloat
  def asDouble: Double = data.toDouble
  def asNumber: Number = data

object LongTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[LongTag] = (MinCache to MaxCache).map(data => new LongTag(data))
  def apply(data: Long): LongTag = Cache.lift(data.toInt - MinCache).getOrElse(new LongTag(data))

final case class FloatTag private (data: Float) extends NumericTag:
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = data.toLong
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object FloatTag:
  val Zero: FloatTag = new FloatTag(0.0f)
  def apply(data: Float): FloatTag = if data == 0.0f then Zero else new FloatTag(data)

final case class DoubleTag private (data: Double) extends NumericTag:
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = Math.floor(data).toLong
  def asFloat: Float = data.toFloat
  def asDouble: Double = data
  def asNumber: Number = data

object DoubleTag:
  val Zero: DoubleTag = new DoubleTag(0.0)
  def apply(data: Double): DoubleTag = if data == 0.0 then Zero else new DoubleTag(data)

sealed trait CollectionTag[T <: Tag] extends Tag with mutable.IndexedBuffer[T]:
  def set(index: Int, tag: Tag): Boolean
  def add(index: Int, tag: Tag): Boolean

final case class ByteArrayTag(private var data: Array[Byte]) extends CollectionTag[ByteTag]:
  override def copy(): ByteArrayTag = ByteArrayTag(data.clone)

  override def equals(argument: Any): Boolean =
    argument match
    case argument: ByteArrayTag => data sameElements argument.data
    case _ => false

  override def addOne(element: ByteTag) =
    data :+= element.asByte; this

  override def apply(index: Int): ByteTag = ByteTag(data(index))

  override def clear(): Unit = data = Array.empty

  override def insert(index: Int, element: ByteTag): Unit =
    val (left, right) = data.splitAt(index)
    data = (left :+ element.asByte) ++ right

  override def insertAll(index: Int, elements: IterableOnce[ByteTag]): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ elements.iterator.map(_.asByte) ++ right

  override def length: Int = data.size

  override def prepend(element: ByteTag) =
    data +:= element.asByte; this

  override def remove(index: Int, count: Int): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ right.drop(count)

  override def remove(index: Int): ByteTag =
    val old = data(index)
    remove(index, 1)
    ByteTag(old)

  override def update(index: Int, element: ByteTag): Unit =
    data(index) = element.asByte

  def set(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => this(index) = ByteTag(tag.asByte); true
    case _ => false

  def add(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => insert(index, ByteTag(tag.asByte)); true
    case _ => false

final case class IntArrayTag(private var data: Array[Int]) extends CollectionTag[IntTag]:
  override def copy(): IntArrayTag = IntArrayTag(data.clone)

  override def equals(argument: Any): Boolean =
    argument match
    case argument: IntArrayTag => data sameElements argument.data
    case _ => false

  override def addOne(element: IntTag) =
    data :+= element.asByte; this

  override def apply(index: Int): IntTag = IntTag(data(index))

  override def clear(): Unit = data = Array.empty

  override def insert(index: Int, element: IntTag): Unit =
    val (left, right) = data.splitAt(index)
    data = (left :+ element.asInt) ++ right

  override def insertAll(index: Int, elements: IterableOnce[IntTag]): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ elements.iterator.map(_.asInt) ++ right

  override def length: Int = data.size

  override def prepend(element: IntTag) =
    data +:= element.asInt; this

  override def remove(index: Int, count: Int): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ right.drop(count)

  override def remove(index: Int): IntTag =
    val old = data(index)
    remove(index, 1)
    IntTag(old)

  override def update(index: Int, element: IntTag): Unit =
    data(index) = element.asInt

  def set(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => this(index) = IntTag(tag.asInt); true
    case _ => false

  def add(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => insert(index, IntTag(tag.asInt)); true
    case _ => false

final case class LongArrayTag(private var data: Array[Long]) extends CollectionTag[LongTag]:
  override def copy(): LongArrayTag = LongArrayTag(data.clone)

  override def equals(argument: Any): Boolean =
    argument match
    case argument: LongArrayTag => data sameElements argument.data
    case _ => false

  override def addOne(element: LongTag) =
    data :+= element.asByte; this

  override def apply(index: Int): LongTag = LongTag(data(index))

  override def clear(): Unit = data = Array.empty

  override def insert(index: Int, element: LongTag): Unit =
    val (left, right) = data.splitAt(index)
    data = (left :+ element.asLong) ++ right

  override def insertAll(index: Int, elements: IterableOnce[LongTag]): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ elements.iterator.map(_.asLong) ++ right

  override def length: Int = data.size

  override def prepend(element: LongTag) =
    data +:= element.asLong; this

  override def remove(index: Int, count: Int): Unit =
    val (left, right) = data.splitAt(index)
    data = left ++ right.drop(count)

  override def remove(index: Int): LongTag =
    val old = data(index)
    remove(index, 1)
    LongTag(old)

  override def update(index: Int, element: LongTag): Unit =
    data(index) = element.asLong

  def set(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => this(index) = LongTag(tag.asLong); true
    case _ => false

  def add(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => insert(index, LongTag(tag.asLong)); true
    case _ => false

final case class ListTag private (data: mutable.Buffer[Tag], private var elementType: Class[? <: Tag]) extends CollectionTag[Tag]:
  override def copy(): ListTag = ListTag(mutable.ArrayBuffer.empty ++ data.map(_.copy()), elementType)

  override def addOne(element: Tag) =
    insert(size, element); this

  override def apply(index: Int): Tag = data(index)

  override def clear(): Unit =
    data.clear()
    updateTypeAfterRemove()

  override def insert(index: Int, element: Tag): Unit =
    if updateType(element) then data.insert(index, element)
    else throw UnsupportedOperationException()

  override def insertAll(index: Int, elements: IterableOnce[Tag]): Unit =
    elements.iterator.foreach(insert(index, _))

  override def length: Int = data.size

  override def prepend(element: Tag) =
    insert(0, element); this

  override def remove(index: Int, count: Int): Unit =
    data.remove(index, count)
    updateTypeAfterRemove()

  override def remove(index: Int): Tag =
    val old = data.remove(index)
    updateTypeAfterRemove()
    old

  override def update(index: Int, element: Tag): Unit =
    set(index, element)

  def set(index: Int, tag: Tag): Boolean =
    if !updateType(tag) then false
    else this(index) = tag; true

  def add(index: Int, tag: Tag): Boolean =
    if !updateType(tag) then false
    else insert(index, tag); true

  private def updateTypeAfterRemove(): Unit =
    if isEmpty then elementType = EndTag.getClass

  private def updateType(tag: Tag): Boolean =
    if tag.getClass == EndTag.getClass then false
    else if elementType == EndTag.getClass then
      elementType = tag.getClass; true
    else elementType == tag.getClass

object ListTag:
  def apply(): ListTag = ListTag(mutable.ArrayBuffer.empty, EndTag.getClass)
