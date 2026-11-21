package com.example.domain.templates

import com.example.domain.model.AnchorSide
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.FlowTemplate
import com.example.domain.model.GridStyle
import com.example.domain.model.NodeShape

object FlowTemplates {

    val categories = listOf("All", "Algorithms", "Software", "Business", "Logic")

    val allTemplates: List<FlowTemplate> = listOf(
        loginProcessTemplate(),
        simpleAlgorithmTemplate(),
        studentRegistrationTemplate(),
        atmWorkflowTemplate(),
        ecommerceOrderTemplate(),
        decisionMakingTemplate(),
        programmingLoopsTemplate(),
        sdlcTemplate(),
    )

    private fun loginProcessTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "n1", shape = NodeShape.OVAL, text = "Start", x = 220f, y = 40f, width = 140f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "n2", shape = NodeShape.PARALLELOGRAM, text = "Enter Email & Password", x = 180f, y = 140f, width = 220f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "n3", shape = NodeShape.DIAMOND, text = "Credentials Valid?", x = 196f, y = 252f, width = 188f, height = 116f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "n4", shape = NodeShape.RECTANGLE, text = "Show Dashboard", x = 40f, y = 420f, width = 176f, height = 72f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "n5", shape = NodeShape.RECTANGLE, text = "Show Login Error", x = 360f, y = 420f, width = 176f, height = 72f, fillColorHex = "#FFE4BA"),
            FlowNode(id = "n6", shape = NodeShape.OVAL, text = "End", x = 220f, y = 550f, width = 140f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "e1", fromNodeId = "n1", toNodeId = "n2"),
            FlowEdge(id = "e2", fromNodeId = "n2", toNodeId = "n3"),
            FlowEdge(id = "e3", fromNodeId = "n3", toNodeId = "n4", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "e4", fromNodeId = "n3", toNodeId = "n5", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "e5", fromNodeId = "n5", toNodeId = "n2", label = "Retry", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.RIGHT, style = EdgeStyle.ORTHOGONAL),
            FlowEdge(id = "e6", fromNodeId = "n4", toNodeId = "n6", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
        )
        return FlowTemplate(
            id = "tpl_login",
            title = "Login Process",
            description = "User authentication flow with credential validation, dashboard redirect, and retry loop.",
            category = "Software",
            badge = "6 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun simpleAlgorithmTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "a1", shape = NodeShape.OVAL, text = "Start", x = 220f, y = 40f, width = 140f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "a2", shape = NodeShape.PARALLELOGRAM, text = "Input A, B", x = 190f, y = 140f, width = 200f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "a3", shape = NodeShape.DIAMOND, text = "Is A > B?", x = 206f, y = 250f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "a4", shape = NodeShape.PARALLELOGRAM, text = "Output A is Max", x = 40f, y = 410f, width = 180f, height = 68f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "a5", shape = NodeShape.PARALLELOGRAM, text = "Output B is Max", x = 350f, y = 410f, width = 180f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "a6", shape = NodeShape.OVAL, text = "End", x = 220f, y = 536f, width = 140f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "ae1", fromNodeId = "a1", toNodeId = "a2"),
            FlowEdge(id = "ae2", fromNodeId = "a2", toNodeId = "a3"),
            FlowEdge(id = "ae3", fromNodeId = "a3", toNodeId = "a4", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "ae4", fromNodeId = "a3", toNodeId = "a5", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "ae5", fromNodeId = "a4", toNodeId = "a6", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
            FlowEdge(id = "ae6", fromNodeId = "a5", toNodeId = "a6", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.RIGHT),
        )
        return FlowTemplate(
            id = "tpl_simple_algo",
            title = "Simple Algorithm",
            description = "Classic maximum-of-two-numbers comparison algorithm ideal for CS beginners.",
            category = "Algorithms",
            badge = "6 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun studentRegistrationTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "s1", shape = NodeShape.OVAL, text = "Start", x = 220f, y = 40f, width = 140f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "s2", shape = NodeShape.PARALLELOGRAM, text = "Fill Student Form", x = 180f, y = 140f, width = 220f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "s3", shape = NodeShape.DIAMOND, text = "Eligible & Seats?", x = 190f, y = 250f, width = 200f, height = 112f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "s4", shape = NodeShape.RECTANGLE, text = "Verify Documents", x = 40f, y = 420f, width = 184f, height = 72f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "s5", shape = NodeShape.RECTANGLE, text = "Add to Waitlist", x = 356f, y = 420f, width = 176f, height = 72f, fillColorHex = "#FFE4BA"),
            FlowNode(id = "s6", shape = NodeShape.RECTANGLE, text = "Issue Student ID", x = 40f, y = 540f, width = 184f, height = 72f, fillColorHex = "#E5D9FF"),
            FlowNode(id = "s7", shape = NodeShape.OVAL, text = "End", x = 220f, y = 668f, width = 140f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "se1", fromNodeId = "s1", toNodeId = "s2"),
            FlowEdge(id = "se2", fromNodeId = "s2", toNodeId = "s3"),
            FlowEdge(id = "se3", fromNodeId = "s3", toNodeId = "s4", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "se4", fromNodeId = "s3", toNodeId = "s5", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "se5", fromNodeId = "s4", toNodeId = "s6"),
            FlowEdge(id = "se6", fromNodeId = "s6", toNodeId = "s7", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
            FlowEdge(id = "se7", fromNodeId = "s5", toNodeId = "s7", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.RIGHT),
        )
        return FlowTemplate(
            id = "tpl_student_reg",
            title = "Student Registration System",
            description = "End-to-end enrollment pipeline covering application, eligibility check, waitlist, and ID generation.",
            category = "Business",
            badge = "7 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun atmWorkflowTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "t1", shape = NodeShape.OVAL, text = "Insert Card", x = 220f, y = 40f, width = 156f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "t2", shape = NodeShape.PARALLELOGRAM, text = "Enter 4-Digit PIN", x = 188f, y = 140f, width = 220f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "t3", shape = NodeShape.DIAMOND, text = "PIN Valid?", x = 214f, y = 250f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "t4", shape = NodeShape.PARALLELOGRAM, text = "Enter Amount", x = 50f, y = 410f, width = 180f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "t5", shape = NodeShape.RECTANGLE, text = "Eject Card & Alert", x = 360f, y = 410f, width = 184f, height = 72f, fillColorHex = "#FFE4BA"),
            FlowNode(id = "t6", shape = NodeShape.DIAMOND, text = "Sufficient Funds?", x = 44f, y = 528f, width = 192f, height = 112f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "t7", shape = NodeShape.RECTANGLE, text = "Dispense Cash & Receipt", x = 44f, y = 694f, width = 192f, height = 76f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "t8", shape = NodeShape.OVAL, text = "End", x = 232f, y = 824f, width = 140f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "te1", fromNodeId = "t1", toNodeId = "t2"),
            FlowEdge(id = "te2", fromNodeId = "t2", toNodeId = "t3"),
            FlowEdge(id = "te3", fromNodeId = "t3", toNodeId = "t4", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "te4", fromNodeId = "t3", toNodeId = "t5", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "te5", fromNodeId = "t4", toNodeId = "t6"),
            FlowEdge(id = "te6", fromNodeId = "t6", toNodeId = "t7", label = "Yes"),
            FlowEdge(id = "te7", fromNodeId = "t6", toNodeId = "t5", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.BOTTOM),
            FlowEdge(id = "te8", fromNodeId = "t7", toNodeId = "t8", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
            FlowEdge(id = "te9", fromNodeId = "t5", toNodeId = "t8", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.RIGHT),
        )
        return FlowTemplate(
            id = "tpl_atm",
            title = "ATM Workflow",
            description = "Banking cash withdrawal workflow with PIN verification, balance validation, and cash dispensing.",
            category = "Business",
            badge = "8 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun ecommerceOrderTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "ec1", shape = NodeShape.OVAL, text = "Order Placed", x = 220f, y = 40f, width = 160f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "ec2", shape = NodeShape.DIAMOND, text = "In Stock?", x = 216f, y = 148f, width = 168f, height = 104f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "ec3", shape = NodeShape.RECTANGLE, text = "Process Payment", x = 50f, y = 304f, width = 180f, height = 72f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "ec4", shape = NodeShape.RECTANGLE, text = "Notify Out of Stock", x = 360f, y = 304f, width = 184f, height = 72f, fillColorHex = "#FFE4BA"),
            FlowNode(id = "ec5", shape = NodeShape.DIAMOND, text = "Payment Paid?", x = 56f, y = 424f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "ec6", shape = NodeShape.RECTANGLE, text = "Pack & Ship Order", x = 50f, y = 584f, width = 180f, height = 72f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "ec7", shape = NodeShape.RECTANGLE, text = "Cancel & Refund", x = 360f, y = 584f, width = 184f, height = 72f, fillColorHex = "#E5D9FF"),
            FlowNode(id = "ec8", shape = NodeShape.OVAL, text = "Order Complete", x = 210f, y = 710f, width = 180f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "ece1", fromNodeId = "ec1", toNodeId = "ec2"),
            FlowEdge(id = "ece2", fromNodeId = "ec2", toNodeId = "ec3", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "ece3", fromNodeId = "ec2", toNodeId = "ec4", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "ece4", fromNodeId = "ec3", toNodeId = "ec5"),
            FlowEdge(id = "ece5", fromNodeId = "ec5", toNodeId = "ec6", label = "Yes"),
            FlowEdge(id = "ece6", fromNodeId = "ec5", toNodeId = "ec7", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "ece7", fromNodeId = "ec6", toNodeId = "ec8", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
            FlowEdge(id = "ece8", fromNodeId = "ec7", toNodeId = "ec8", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.RIGHT),
            FlowEdge(id = "ece9", fromNodeId = "ec4", toNodeId = "ec8", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.RIGHT),
        )
        return FlowTemplate(
            id = "tpl_ecommerce",
            title = "E-commerce Order Process",
            description = "Full retail fulfillment flow from checkout through inventory check, payment, and shipping.",
            category = "Business",
            badge = "8 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun decisionMakingTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "d1", shape = NodeShape.OVAL, text = "Start", x = 240f, y = 40f, width = 140f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "d2", shape = NodeShape.DIAMOND, text = "Is It Urgent?", x = 226f, y = 148f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "d3", shape = NodeShape.DIAMOND, text = "High Impact?", x = 50f, y = 316f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "d4", shape = NodeShape.RECTANGLE, text = "Schedule Later", x = 400f, y = 334f, width = 172f, height = 72f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "d5", shape = NodeShape.RECTANGLE, text = "Do Immediately", x = 20f, y = 490f, width = 172f, height = 72f, fillColorHex = "#FA93B2"),
            FlowNode(id = "d6", shape = NodeShape.RECTANGLE, text = "Delegate Task", x = 224f, y = 490f, width = 164f, height = 72f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "d7", shape = NodeShape.OVAL, text = "Done", x = 240f, y = 626f, width = 140f, height = 60f, fillColorHex = "#E5D9FF"),
        )
        val edges = listOf(
            FlowEdge(id = "de1", fromNodeId = "d1", toNodeId = "d2"),
            FlowEdge(id = "de2", fromNodeId = "d2", toNodeId = "d3", label = "Yes", fromSide = AnchorSide.LEFT, toSide = AnchorSide.TOP),
            FlowEdge(id = "de3", fromNodeId = "d2", toNodeId = "d4", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "de4", fromNodeId = "d3", toNodeId = "d5", label = "Yes", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.TOP),
            FlowEdge(id = "de5", fromNodeId = "d3", toNodeId = "d6", label = "No", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.TOP),
            FlowEdge(id = "de6", fromNodeId = "d5", toNodeId = "d7", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.LEFT),
            FlowEdge(id = "de7", fromNodeId = "d6", toNodeId = "d7"),
            FlowEdge(id = "de8", fromNodeId = "d4", toNodeId = "d7", fromSide = AnchorSide.BOTTOM, toSide = AnchorSide.RIGHT),
        )
        return FlowTemplate(
            id = "tpl_decision",
            title = "Decision Making",
            description = "Nested Eisenhower priority decision tree separating urgent and high-impact actions.",
            category = "Logic",
            badge = "7 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun programmingLoopsTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "l1", shape = NodeShape.OVAL, text = "Start", x = 220f, y = 40f, width = 140f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "l2", shape = NodeShape.RECTANGLE, text = "Set i = 1, sum = 0", x = 196f, y = 140f, width = 188f, height = 68f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "l3", shape = NodeShape.DIAMOND, text = "i <= 10 ?", x = 206f, y = 252f, width = 168f, height = 108f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "l4", shape = NodeShape.RECTANGLE, text = "sum = sum + i\ni = i + 1", x = 196f, y = 420f, width = 188f, height = 76f, fillColorHex = "#E5D9FF"),
            FlowNode(id = "l5", shape = NodeShape.PARALLELOGRAM, text = "Print sum", x = 440f, y = 272f, width = 160f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "l6", shape = NodeShape.OVAL, text = "End", x = 450f, y = 428f, width = 140f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "le1", fromNodeId = "l1", toNodeId = "l2"),
            FlowEdge(id = "le2", fromNodeId = "l2", toNodeId = "l3"),
            FlowEdge(id = "le3", fromNodeId = "l3", toNodeId = "l4", label = "True"),
            FlowEdge(id = "le4", fromNodeId = "l4", toNodeId = "l3", label = "Loop", fromSide = AnchorSide.LEFT, toSide = AnchorSide.LEFT),
            FlowEdge(id = "le5", fromNodeId = "l3", toNodeId = "l5", label = "False", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.LEFT),
            FlowEdge(id = "le6", fromNodeId = "l5", toNodeId = "l6"),
        )
        return FlowTemplate(
            id = "tpl_loops",
            title = "Programming Loops",
            description = "Structured while/for loop accumulator diagram demonstrating loop condition and back-edge iteration.",
            category = "Algorithms",
            badge = "6 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }

    private fun sdlcTemplate(): FlowTemplate {
        val nodes = listOf(
            FlowNode(id = "sd1", shape = NodeShape.OVAL, text = "Project Kickoff", x = 210f, y = 40f, width = 176f, height = 60f, fillColorHex = "#BEF6AC"),
            FlowNode(id = "sd2", shape = NodeShape.RECTANGLE, text = "Requirements", x = 204f, y = 140f, width = 188f, height = 68f, fillColorHex = "#D4EFF0"),
            FlowNode(id = "sd3", shape = NodeShape.RECTANGLE, text = "System Design", x = 204f, y = 248f, width = 188f, height = 68f, fillColorHex = "#E5D9FF"),
            FlowNode(id = "sd4", shape = NodeShape.RECTANGLE, text = "Implementation", x = 204f, y = 356f, width = 188f, height = 68f, fillColorHex = "#F1F0D4"),
            FlowNode(id = "sd5", shape = NodeShape.DIAMOND, text = "QA Tests Pass?", x = 206f, y = 468f, width = 184f, height = 112f, fillColorHex = "#A3E9CC"),
            FlowNode(id = "sd6", shape = NodeShape.RECTANGLE, text = "Deploy Release", x = 204f, y = 632f, width = 188f, height = 68f, fillColorHex = "#FFE4BA"),
            FlowNode(id = "sd7", shape = NodeShape.CONNECTOR, text = "M", x = 440f, y = 634f, width = 64f, height = 64f, fillColorHex = "#E5D9FF"),
            FlowNode(id = "sd8", shape = NodeShape.OVAL, text = "Maintenance", x = 210f, y = 748f, width = 176f, height = 60f, fillColorHex = "#FA93B2"),
        )
        val edges = listOf(
            FlowEdge(id = "sde1", fromNodeId = "sd1", toNodeId = "sd2"),
            FlowEdge(id = "sde2", fromNodeId = "sd2", toNodeId = "sd3"),
            FlowEdge(id = "sde3", fromNodeId = "sd3", toNodeId = "sd4"),
            FlowEdge(id = "sde4", fromNodeId = "sd4", toNodeId = "sd5"),
            FlowEdge(id = "sde5", fromNodeId = "sd5", toNodeId = "sd4", label = "Fix Bugs", fromSide = AnchorSide.LEFT, toSide = AnchorSide.LEFT),
            FlowEdge(id = "sde6", fromNodeId = "sd5", toNodeId = "sd6", label = "Pass"),
            FlowEdge(id = "sde7", fromNodeId = "sd6", toNodeId = "sd7", label = "Monitor", fromSide = AnchorSide.RIGHT, toSide = AnchorSide.LEFT),
            FlowEdge(id = "sde8", fromNodeId = "sd6", toNodeId = "sd8"),
        )
        return FlowTemplate(
            id = "tpl_sdlc",
            title = "Software Development Lifecycle",
            description = "Complete engineering lifecycle from requirements and design to QA testing loop and release.",
            category = "Software",
            badge = "8 nodes",
            graph = FlowGraph(nodes = nodes, edges = edges, gridStyle = GridStyle.PLUS),
        )
    }
}
