{{class.description}}

{% for method in methods %}

#### {{method.name}}

{{method.description | trim | raw }}


[&raquo; full source]({{class.path}}/{{class.name}}.java#L{{method.range.begin.line}}C{{method.range.begin.column}}-L{{method.range.end.line}}C{{method.range.end.column}})

```java
{{method.body | trim | raw }}

```

{% endfor %}
