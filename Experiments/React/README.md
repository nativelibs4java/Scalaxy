
http://facebook.github.io/react/

```js
var HelloMessage = React.createClass({
  render: function() {
    return <div>Hello {this.props.name}</div>;
  }
});

React.render(<HelloMessage name="John" />, mountNode);
```

```scala
val HelloMessage = React.createClass(
  render = () => <div>Hello {this.props.name}</div>
)

React.render(<HelloMessage name="John" />, mountNode)
```
