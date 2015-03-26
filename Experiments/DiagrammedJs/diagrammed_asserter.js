/**
 * @fileoverview
 * This file contains a diagrammed asserter for JavaScript / ES6, similar to
 * Scala libraries such as scalatest's DiagrammedAssertions and expecty.
 *
 * It works best with the arrow function syntax.
 * (In Chrome, experimental JavaScript must be enabled under chrome://flags)
 *
 * It assumes UglifyJS is in scope (see http://lisperator.net/uglifyjs/ for instructions).
 *
 * Example Usage (the diagrams will be displayed in an exception):
 *
 *   var assert = asserter(_ => eval(_));
 *
 *   var s = 'abc';
 *   assert(_ => s.indexOf("d") >= 0);
 *   //          | |            |
 *   //          | -1           false
 *   //          'abc'
 *
 *   var a = 1, b = 2, c = 3, d = 4;
 *   assert(_ => a == b || c >= d)
 *   //          | |  | |  | |  |
 *   //          1 |  2 |  3 |  4
 *   //            |    |    false
 *   //            |    false
 *   //            false
 *
 *   var o = {a: {b: {c: 10}}};
 *   assert(_ => o['a'] == 11)
 *   //          | |    |
 *   //          | |    false
 *   //          | {"b":{"c":10}}
 *   //          {"a":{"b":{"c":10}}}
 */

/**
 * @struct @constructor
 * @param {function(string):*} evaluator
 */
var Asserter = function(evaluator) {
  this.evaluator = evaluator;
};

/**
 * @struct @constructor
 * @param {!string} fragment
 * @param {number} pos
 */
Asserter.Value = function(fragment, pos) {
  this.fragment = fragment;
  this.pos = pos;
};

/**
 * @param {!string} src
 * @return {!UglifyJS.AST_Node}
 */
Asserter.parse = function(src) {
  var check = function(test) {
    if (!test) throw new Error();
  };
  var ast = UglifyJS.parse(src);
  check(ast instanceof UglifyJS.AST_Toplevel);
  check(ast.body.length == 1);
  ast = ast.body[0];
  check(ast instanceof UglifyJS.AST_SimpleStatement);
  return ast.body;
};

Asserter.skipChar = function(c, pos, str) {
  if (pos >= 0) {
    while (str.charAt(pos) == c) {
      pos++;
    }
  }
  return pos;
};

/**
 * Walk down the AST and collect values.
 *
 * See http://lisperator.net/uglifyjs/ast
 *
 * @param {string} src
 * @param {!UglifyJS.AST_Node} node
 * @param {!Array.<!Asserter.Value>} out
 * @return {!Array.<!Asserter.Value>}
 */
Asserter.collectValues = function(src, node, out) {
  var fragment = src.substring(node.start.pos, node.end.endpos);
  var recurse = function(sub) {
    Asserter.collectValues(src, sub, out);
  };
  var pos = -1;
  if (node instanceof UglifyJS.AST_Binary) {
    recurse(node.left);
    recurse(node.right);
    pos = node.left.end.endpos;
  } else if (node instanceof UglifyJS.AST_Call) {
    if (node.expression instanceof UglifyJS.AST_PropAccess) {
      recurse(node.expression.expression);
      pos = node.expression.end.pos;
    }
    node.args.forEach(recurse);
  } else if (node instanceof UglifyJS.AST_PropAccess) {
    recurse(node.expression);
    pos = node.expression.end.endpos;
    pos = Asserter.skipChar(' ', pos, src);
    var c = (node instanceof UglifyJS.AST_Dot) ? '.' : '[';
    pos = Asserter.skipChar(c, pos, src);
  } else if (!(node instanceof UglifyJS.AST_Constant)) {
    pos = node.start.pos;
  }
  pos = Asserter.skipChar(' ', pos, src);
  if (pos >= 0) {
    out.push(new Asserter.Value(fragment, pos));
  }
  return out;
};

/** @private */
Asserter._ARROW_FUNC_RX = /^_[\s\n]*=>[\s\n]*/;
/** @private */
Asserter._FUNC_RX = /^function[\s\S]*?\{[\s\n]*return[\s\n]*(.*?);?[\s\n]*\}$/;

/**
 * @param {Function} fun
 * @return {!string}
 */
Asserter.stripFunctionBody = function(fun) {
  var src = fun.toString().trim();
  if (src.match(Asserter._ARROW_FUNC_RX)) {
    return src.replace(Asserter._ARROW_FUNC_RX, '').trim();
  } else {
    return src.replace(Asserter._FUNC_RX, '$1').trim();
  }
};

/** @private @const */
Asserter._MAX_STRINGIFIED_LENGTH = 20;

/**
 * @param {*} value
 * @return {!string}
 */
Asserter._stringify = function(value) {
  if (typeof value == "string") {
    return "'" + value + "'";
  } else {
    if (typeof value == "object" &&
        value.toString === Object.prototype.toString) {
      try {
        var s = JSON.stringify(value);
        if (s.length > Asserter._MAX_STRINGIFIED_LENGTH) {
          var closingChar;
          switch (s[0]) {
            case '{': closingChar = '}'; break;
            case '[': closingChar = ']'; break;
            default:  closingChar = ' '; break;
          }
          s = s.substr(0, Asserter._MAX_STRINGIFIED_LENGTH) + '...' + closingChar;
        }
        return s;
      } catch (e) {
        // Cyclic references? Do nothing.
      }
    }
    // Add an extra space for booleans and numbers.
    return String(value) + ' ';
  }
};

/**
 * @param {function():boolean} testFun
 */
Asserter.prototype.assert = function(testFun) {
  var result = testFun();
  if ((typeof result) != "boolean") {
    throw new Error("Expected boolean result for " + testFun + ", got: " + result);
  }
  if (result) {
    return;
  }
  var src = Asserter.stripFunctionBody(testFun);
  var ast = Asserter.parse(src);
  var values = Asserter.collectValues(src, ast, []);
  var diagram = src + "\n" + this.drawValues(values);

  var msg = "Assertion failed:\n\n\t" + diagram.replace(/\n/g, "\n\t") + "\n";
  //throw new Error(msg)
  console.log(new Error(msg).stack);
};

/**
 * @param {function():boolean} testFun
 */
Asserter.prototype.drawValues = function(values) {
  var canvas = new Asserter.LinesCanvas();
  // Order values from right to left.
  values.sort(function(a, b) {
    return b.pos - a.pos;
  });
  values.forEach(function(node) {
    var value = Asserter._stringify(this.evaluator(node.fragment));
    var col = node.pos;
    for (var row = 0; row <= canvas.lines.length; row++) {
      if (row > 0 && canvas.draw(value, row, col)) {
        return;
      }
      canvas.draw('|', row, col);
    }
    canvas.draw(value, canvas.lines.length, col);
  }, this);
  return canvas;
};

/** @struct @constructor */
Asserter.LinesCanvas = function() {
  /** @type {!Array.<!string>} */
  this.lines = [];
};

/**
 * Draw a string value at the given row and col, if there is nothing drawn already.
 * @param {!string} value
 * @param {number} row
 * @param {number} col
 * @return {boolean} whether the draw was successful.
 */
Asserter.LinesCanvas.prototype.draw = function(value, row, col) {
  var lines = this.lines;
  for (var i = lines.length; i <= row; i++) {
    lines.push('');
  }
  var line = lines[row];
  for (var j = line.length; j <= col; j++) {
    line += ' ';
  }
  var canDraw = !!line.substr(col, value.length).match(/^\s*$/);
  if (canDraw) {
    lines[row] = line.substr(0, col) + value + line.substr(col + value.length);
  }
  return canDraw;
};

/** @override */
Asserter.LinesCanvas.prototype.toString = function() {
  return this.lines.join('\n');
};

/** @export */
var asserter = function(evaluator) {
  return function(test) {
    return new Asserter(evaluator).assert(test);
  };
};
