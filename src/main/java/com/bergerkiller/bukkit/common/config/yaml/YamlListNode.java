package com.bergerkiller.bukkit.common.config.yaml;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.bergerkiller.bukkit.common.collections.AbstractListProxy;

public class YamlListNode extends YamlNode implements List<Object> {

    public YamlListNode() {
    }

    protected YamlListNode(YamlEntry entry) {
        super(entry);
    }

    @Override
    protected YamlEntry removeChildEntryAt(int index) {
        YamlEntry removed = super.removeChildEntryAt(index);

        // Item was removed, regenerate the paths for the entries that come after
        // Because index for these values went down by 1, do this in normal order
        for (int i = index; i <  this._children.size(); i++) {
            this._children.get(i).setPath(this.getYamlPath().listChild(i));
        }
        return removed;
    }

    @Override
    protected YamlEntry createChildEntry(int index, YamlPath path) {
        YamlEntry created = super.createChildEntry(index, path);

        // Item was inserted, regenerate the paths for the entries that come after
        // Because index for these values went up by 1, do this in reverse order
        for (int i = this._children.size()-1; i >= index; i--) {
            this._children.get(i).setPath(this.getYamlPath().listChild(i));
        }

        return created;
    }

    @Override
    public int size() {
        return this._children.size();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < _children.size();
            }

            @Override
            public Object next() {
                if (index >= _children.size()) {
                    throw new NoSuchElementException("No next element is available");
                }
                return _children.get(index++).getValue();
            }

            @Override
            public void remove() {
                YamlListNode.this.remove(index-1);
            }
        };
    }

    @Override
    public Object get(int index) {
        return this._children.get(index).getValue();
    }

    @Override
    public Object set(int index, Object element) {
        YamlEntry entry = this._children.get(index);
        Object oldValue = entry.getValue();
        entry.setValue(element);
        return oldValue;
    }

    @Override
    public boolean add(Object e) {
        add(this._children.size(), e);
        return true;
    }

    @Override
    public void add(int index, Object element) {
        this.createChildEntry(index, this.getYamlPath().listChild(index)).setValue(element);
    }

    @Override
    public boolean remove(Object o) {
        int index = this.indexOf(o);
        if (index != -1) {
            this.removeChildEntryAt(index);
            return true;
        }
        return false;
    }

    @Override
    public Object remove(int index) {
        return this.removeChildEntryAt(index).getValue();
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < this._children.size(); i++) {
                if (this._children.get(i).getValue() == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < this._children.size(); i++) {
                if (o.equals(this._children.get(i).getValue())) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = this._children.size()-1; i >= 0; i--) {
                if (this._children.get(i).getValue() == null) {
                    return i;
                }
            }
        } else {
            for (int i = this._children.size()-1; i >= 0; i--) {
                if (o.equals(this._children.get(i).getValue())) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[this._children.size()];
        for (int i = 0; i < this._children.size(); i++) {
            result[i] = this._children.get(i).getValue();
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = this._children.size();
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        } else if (size > a.length) {
            a[size] = null;
        }
        for (int i = 0; i < size; i++) {
            a[i] = (T) this._children.get(i).getValue();
        }
        return a;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Object> c) {
        return addAll(this._children.size(), c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Object> c) {
        // Add all children without calling the path renaming logic
        for (Object element : c) {
            super.createChildEntry(index, this.getYamlPath().listChild(index)).setValue(element);
            index++;
        }

        // Update all paths in one go, in reverse order (see createChildEntry() above)
        for (int i = this._children.size()-1; i >= index; i--) {
            this._children.get(i).setPath(this.getYamlPath().listChild(i));
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.removeOrRetainAll(c, true);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.removeOrRetainAll(c, false);
    }

    private boolean removeOrRetainAll(Collection<?> c, boolean remove) {
        int first_mod = Integer.MAX_VALUE;

        // Remove all children that are/aren't contained inside the collection
        int i = this._children.size();
        while (--i >= 0) {
            if (remove == c.contains(this._children.get(i).getValue())) {
                first_mod = i;
                super.removeChildEntryAt(i);
            }
        }

        // Update the paths of all the children that come after in natural order (see removeChildEntryAt above)
        // If there were no changes then first_mod is MAX_VALUE, and the condition won't be true ever
        for (i = first_mod; i < this._children.size(); i++) {
            this._children.get(i).setPath(this.getYamlPath().listChild(i));
        }
        return first_mod != Integer.MAX_VALUE;
    }

    @Override
    public ListIterator<Object> listIterator() {
        return AbstractListProxy.create(this).listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return AbstractListProxy.create(this).listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return AbstractListProxy.create(this).subList(fromIndex, toIndex);
    }
}
