'use strict';(function(g){"object"==typeof exports&&"object"==typeof module?g(require("../../lib/codemirror")):"function"==typeof define&&define.amd?define(["../../lib/codemirror"],g):g(CodeMirror)})(function(g){function f(a,b,c,d){this.cm=a;this.node=b;this.options=c;this.height=d;this.cleared=!1}function h(a){var b=a.getWrapperElement(),c=window.getComputedStyle?window.getComputedStyle(b):b.currentStyle,d=parseInt(c.height),e=a.state.panels={setHeight:b.style.height,heightLeft:d,panels:0,wrapper:document.createElement("div")};
b.parentNode.insertBefore(e.wrapper,b);c=a.hasFocus();e.wrapper.appendChild(b);c&&a.focus();a._setSize=a.setSize;null!=d&&(a.setSize=function(b,c){if(null==c)return this._setSize(b,c);e.setHeight=c;if("number"!=typeof c){var f=/^(\d+\.?\d*)px$/.exec(c);f?c=Number(f[1]):(e.wrapper.style.height=c,c=e.wrapper.offsetHeight,e.wrapper.style.height="")}a._setSize(b,e.heightLeft+=c-d);d=c})}g.defineExtension("addPanel",function(a,b){b=b||{};this.state.panels||h(this);var c=this.state.panels,d=c.wrapper,e=
this.getWrapperElement();b.after instanceof f&&!b.after.cleared?d.insertBefore(a,b.before.node.nextSibling):b.before instanceof f&&!b.before.cleared?d.insertBefore(a,b.before.node):b.replace instanceof f&&!b.replace.cleared?(d.insertBefore(a,b.replace.node),b.replace.clear()):"bottom"==b.position?d.appendChild(a):"before-bottom"==b.position?d.insertBefore(a,e.nextSibling):"after-top"==b.position?d.insertBefore(a,e):d.insertBefore(a,d.firstChild);d=b&&b.height||a.offsetHeight;this._setSize(null,c.heightLeft-=
d);c.panels++;return new f(this,a,b,d)});f.prototype.clear=function(){if(!this.cleared){this.cleared=!0;var a=this.cm.state.panels;this.cm._setSize(null,a.heightLeft+=this.height);a.wrapper.removeChild(this.node);if(0==--a.panels){var a=this.cm,b=a.state.panels;a.state.panels=null;var c=a.getWrapperElement();b.wrapper.parentNode.replaceChild(c,b.wrapper);c.style.height=b.setHeight;a.setSize=a._setSize;a.setSize()}}};f.prototype.changed=function(a){a=null==a?this.node.offsetHeight:a;this.cm._setSize(null,
this.cm.state.panels.height+=a-this.height);this.height=a}});
