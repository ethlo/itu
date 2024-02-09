{{class.description}}

{% for method in methods %}

#### {{method.name}} <span style="font-weight: normal">[&raquo; source]({{class.path}}/{{class.name}}.java#L{{method.range.begin.line}}C{{method.range.begin.column}}-L{{method.range.end.line}}C{{method.range.end.column + 1}})</span>

{{method.description | trim | raw }}

```java
{{method.body | trim | raw }}

```

{% endfor %}
