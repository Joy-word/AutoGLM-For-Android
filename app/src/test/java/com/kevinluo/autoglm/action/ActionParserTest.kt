package com.kevinluo.autoglm.action

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow

/**
 * Unit tests for ActionParser.
 */
class ActionParserTest : DescribeSpec({
    
    describe("ActionParser") {
        
        describe("Tap action parsing") {
            it("should parse basic tap action") {
                val result = ActionParser.parse("""do(action="Tap", element=[500, 300])""")
                result.shouldBeInstanceOf<AgentAction.Tap>()
                val tap = result as AgentAction.Tap
                tap.x shouldBe 500
                tap.y shouldBe 300
                tap.message shouldBe null
            }
            
            it("should parse tap action with message") {
                val result = ActionParser.parse("""do(action="Tap", element=[100, 200], message="确认支付")""")
                result.shouldBeInstanceOf<AgentAction.Tap>()
                val tap = result as AgentAction.Tap
                tap.x shouldBe 100
                tap.y shouldBe 200
                tap.message shouldBe "确认支付"
            }
        }
        
        describe("Swipe action parsing") {
            it("should parse swipe action") {
                val result = ActionParser.parse("""do(action="Swipe", start=[500, 700], end=[500, 300])""")
                result.shouldBeInstanceOf<AgentAction.Swipe>()
                val swipe = result as AgentAction.Swipe
                swipe.startX shouldBe 500
                swipe.startY shouldBe 700
                swipe.endX shouldBe 500
                swipe.endY shouldBe 300
            }
        }
        
        describe("Type action parsing") {
            it("should parse type action with simple text") {
                val result = ActionParser.parse("""do(action="Type", text="Hello World")""")
                result.shouldBeInstanceOf<AgentAction.Type>()
                val type = result as AgentAction.Type
                type.text shouldBe "Hello World"
            }
            
            it("should parse type action with Chinese text") {
                val result = ActionParser.parse("""do(action="Type", text="你好世界")""")
                result.shouldBeInstanceOf<AgentAction.Type>()
                val type = result as AgentAction.Type
                type.text shouldBe "你好世界"
            }
            
            it("should parse type action with empty text") {
                val result = ActionParser.parse("""do(action="Type", text="")""")
                result.shouldBeInstanceOf<AgentAction.Type>()
                val type = result as AgentAction.Type
                type.text shouldBe ""
            }
            
            it("should parse Type_Name action") {
                val result = ActionParser.parse("""do(action="Type_Name", text="张三")""")
                result.shouldBeInstanceOf<AgentAction.TypeName>()
                val typeName = result as AgentAction.TypeName
                typeName.text shouldBe "张三"
            }
        }
        
        describe("Launch action parsing") {
            it("should parse launch action with app name") {
                val result = ActionParser.parse("""do(action="Launch", app="微信")""")
                result.shouldBeInstanceOf<AgentAction.Launch>()
                val launch = result as AgentAction.Launch
                launch.app shouldBe "微信"
            }
            
            it("should parse launch action with package name") {
                val result = ActionParser.parse("""do(action="Launch", app="com.tencent.mm")""")
                result.shouldBeInstanceOf<AgentAction.Launch>()
                val launch = result as AgentAction.Launch
                launch.app shouldBe "com.tencent.mm"
            }
        }
        
        describe("Navigation actions parsing") {
            it("should parse Back action") {
                val result = ActionParser.parse("""do(action="Back")""")
                result shouldBe AgentAction.Back
            }
            
            it("should parse Home action") {
                val result = ActionParser.parse("""do(action="Home")""")
                result shouldBe AgentAction.Home
            }
            
            it("should parse VolumeUp action") {
                val result = ActionParser.parse("""do(action="VolumeUp")""")
                result shouldBe AgentAction.VolumeUp
            }
            
            it("should parse VolumeDown action") {
                val result = ActionParser.parse("""do(action="VolumeDown")""")
                result shouldBe AgentAction.VolumeDown
            }
            
            it("should parse Power action") {
                val result = ActionParser.parse("""do(action="Power")""")
                result shouldBe AgentAction.Power
            }
        }
        
        describe("Long Press action parsing") {
            it("should parse long press action") {
                val result = ActionParser.parse("""do(action="Long Press", element=[300, 400])""")
                result.shouldBeInstanceOf<AgentAction.LongPress>()
                val longPress = result as AgentAction.LongPress
                longPress.x shouldBe 300
                longPress.y shouldBe 400
                longPress.durationMs shouldBe 3000 // default
            }
            
            it("should parse long press action with custom duration") {
                val result = ActionParser.parse("""do(action="Long Press", element=[300, 400], duration="5000")""")
                result.shouldBeInstanceOf<AgentAction.LongPress>()
                val longPress = result as AgentAction.LongPress
                longPress.durationMs shouldBe 5000
            }
        }
        
        describe("Double Tap action parsing") {
            it("should parse double tap action") {
                val result = ActionParser.parse("""do(action="Double Tap", element=[250, 350])""")
                result.shouldBeInstanceOf<AgentAction.DoubleTap>()
                val doubleTap = result as AgentAction.DoubleTap
                doubleTap.x shouldBe 250
                doubleTap.y shouldBe 350
            }
        }
        
        describe("Wait action parsing") {
            it("should parse wait action with seconds") {
                val result = ActionParser.parse("""do(action="Wait", duration="3 seconds")""")
                result.shouldBeInstanceOf<AgentAction.Wait>()
                val wait = result as AgentAction.Wait
                wait.durationSeconds shouldBe 3.0f
            }
            
            it("should parse wait action with numeric duration") {
                val result = ActionParser.parse("""do(action="Wait", duration="2.5")""")
                result.shouldBeInstanceOf<AgentAction.Wait>()
                val wait = result as AgentAction.Wait
                wait.durationSeconds shouldBe 2.5f
            }
        }
        
        describe("Finish action parsing") {
            it("should parse finish action") {
                val result = ActionParser.parse("""finish(message="任务完成")""")
                result.shouldBeInstanceOf<AgentAction.Finish>()
                val finish = result as AgentAction.Finish
                finish.message shouldBe "任务完成"
            }
            
            it("should parse finish action with single quotes") {
                val result = ActionParser.parse("""finish(message='Task completed')""")
                result.shouldBeInstanceOf<AgentAction.Finish>()
                val finish = result as AgentAction.Finish
                finish.message shouldBe "Task completed"
            }
        }
        
        describe("Error handling") {
            it("should throw exception for unknown action format") {
                shouldThrow<ActionParseException> {
                    ActionParser.parse("unknown action")
                }
            }
            
            it("should throw exception for unknown action type") {
                shouldThrow<ActionParseException> {
                    ActionParser.parse("""do(action="UnknownAction")""")
                }
            }
        }
        
        describe("Coordinate validation") {
            it("should throw CoordinateOutOfRangeException for tap with x > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Tap", element=[1000, 500])""")
                }
                exception.invalidCoordinates.size shouldBe 1
                exception.invalidCoordinates[0].name shouldBe "x"
                exception.invalidCoordinates[0].value shouldBe 1000
            }
            
            it("should throw CoordinateOutOfRangeException for tap with y > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Tap", element=[500, 1200])""")
                }
                exception.invalidCoordinates.size shouldBe 1
                exception.invalidCoordinates[0].name shouldBe "y"
                exception.invalidCoordinates[0].value shouldBe 1200
            }
            
            it("should throw CoordinateOutOfRangeException for tap with both coordinates > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Tap", element=[1500, 2000])""")
                }
                exception.invalidCoordinates.size shouldBe 2
            }
            
            it("should throw CoordinateOutOfRangeException for swipe with coordinates > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Swipe", start=[500, 1100], end=[500, 300])""")
                }
                exception.invalidCoordinates.size shouldBe 1
                exception.invalidCoordinates[0].name shouldBe "startY"
                exception.invalidCoordinates[0].value shouldBe 1100
            }
            
            it("should throw CoordinateOutOfRangeException for long press with coordinates > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Long Press", element=[1050, 400])""")
                }
                exception.invalidCoordinates.size shouldBe 1
                exception.invalidCoordinates[0].name shouldBe "x"
                exception.invalidCoordinates[0].value shouldBe 1050
            }
            
            it("should throw CoordinateOutOfRangeException for double tap with coordinates > 999") {
                val exception = shouldThrow<CoordinateOutOfRangeException> {
                    ActionParser.parse("""do(action="Double Tap", element=[250, 1350])""")
                }
                exception.invalidCoordinates.size shouldBe 1
                exception.invalidCoordinates[0].name shouldBe "y"
                exception.invalidCoordinates[0].value shouldBe 1350
            }
            
            it("should accept coordinates at boundary (999)") {
                val result = ActionParser.parse("""do(action="Tap", element=[999, 999])""")
                result.shouldBeInstanceOf<AgentAction.Tap>()
                val tap = result as AgentAction.Tap
                tap.x shouldBe 999
                tap.y shouldBe 999
            }
            
            it("should accept coordinates at boundary (0)") {
                val result = ActionParser.parse("""do(action="Tap", element=[0, 0])""")
                result.shouldBeInstanceOf<AgentAction.Tap>()
                val tap = result as AgentAction.Tap
                tap.x shouldBe 0
                tap.y shouldBe 0
            }
        }
    }
})
