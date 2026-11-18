{{class.description}}

{% for method in methods %}

#### {{method.name}}

<smaller style="float:right;">[source &raquo;]({{method.link}})</smaller>

{{method.description | trim | raw }}

```java
{{method.body | trim | raw }}

```
{% endfor %}
