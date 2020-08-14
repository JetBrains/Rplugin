# Not existing class
new('SomeClass')

# Not string literal
className <- "grouped_df"
new(className)

# Some classes from loaded packages
new('classRepresentation')
new('matrix')

# Some class from packages which doesn't seem to be loaded
new(<warning descr="Unresolved reference">"grouped_df"</warning>) # dplyr package
new(<warning descr="Unresolved reference">"data.table"</warning>) # data.table package