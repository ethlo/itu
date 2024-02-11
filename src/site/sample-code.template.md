{{class.description}}

{% for method in methods %}

#### {{method.name}}

<smaller style="float:right;">[source &raquo;]({{class.path}}/{{class.name}}.java#L{{method.range.begin.line}}C{{method.range.begin.column}}-L{{method.range.end.line}}C{{method.range.end.column + 1}})</smaller>

{{method.description | trim | raw }}

```java
{{method.body | trim | raw }}

```
{% endfor %}
