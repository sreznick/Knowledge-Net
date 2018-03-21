package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookTreeItems : RComponent<ReferenceBookTreeItems.Props, ReferenceBookTreeItems.State>() {

    override fun State.init() {
        addingBookItem = false
    }

    private fun startAddingBookItem(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            addingBookItem = true
        }
    }

    private fun cancelBookItemCreating() {
        setState {
            addingBookItem = false
        }
    }

    private fun createBookItem(bookItemData: ReferenceBookItemData) {
        setState {
            addingBookItem = false
        }
        props.createBookItem(bookItemData)
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--items-block") {
            props.bookItems.map {
                referenceBookTreeItem {
                    attrs {
                        book = props.book
                        key = it.id
                        bookItem = it
                        onBookItemClick = props.onBookItemClick
                        createBookItem = props.createBookItem
                    }
                }
            }
            if (state.addingBookItem) {
                bookItemEditConsole {
                    attrs {
                        bookItem = ReferenceBookItemData(null, "", props.book.id, props.book.name)
                        onCancel = ::cancelBookItemCreating
                        onSubmit = ::createBookItem
                    }
                }
            } else {
                div(classes = "book-tree-view--label book-tree-view--label__selected") {
                    attrs {
                        onClickFunction = ::startAddingBookItem
                    }
                    span(classes = "book-tree-view--empty") {
                        +"(Add Item ...)"
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var bookItems: List<ReferenceBookItem>
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var createBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var addingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeItems(block: RHandler<ReferenceBookTreeItems.Props>) =
    child(ReferenceBookTreeItems::class, block)