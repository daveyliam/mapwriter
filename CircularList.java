package mapwriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CircularList<T> {
	private Map<T, Node> nodeMap = new HashMap<T, Node>();
	private Node headNode = null;
	private Node currentNode = null;
	
	public class Node {
		final T value;
		private Node next;
		private Node prev;
		
		Node(T value) {
			this.value = value;
			this.next = this;
			this.prev = this;
		}
		
		public Node getNext() {
			return this.next;
		}
		
		public Node getPrev() {
			return this.prev;
		}
	}
	
	public boolean contains(T value) {
		return this.nodeMap.containsKey(value);
	}
	
	public int size() {
		return this.nodeMap.size();
	}
	
	public void clear() {
		for (Node node : this.nodeMap.values()) {
			node.next = null;
			node.prev = null;
		}
		this.nodeMap.clear();
		this.headNode = null;
		this.currentNode = null;
	}
	
	public void add(T value) {
		if (!this.nodeMap.containsKey(value)) {
			Node node = new Node(value);
			this.nodeMap.put(value, node);
			
			if (this.headNode == null) {
				node.next = node;
				node.prev = node;
				
			} else {
				node.next = this.headNode.next;
				node.prev = this.headNode;
				
				this.headNode.next.prev = node;
				this.headNode.next = node;
			}
			
			if (this.currentNode == null) {
				this.currentNode = node;
			}
			
			this.headNode = node;
		}
	}
	
	public void remove(T value) {
		Node node = this.nodeMap.get(value);
		if (node != null) {
			if (this.headNode == node) {
				this.headNode = node.next;
				if (this.headNode == node) {
					this.headNode = null;
				}
			}
			if (this.currentNode == node) {
				this.currentNode = node.next;
				if (this.currentNode == node) {
					this.currentNode = null;
				}
			}
			
			node.prev.next = node.next;
			node.next.prev = node.prev;
			node.next = null;
			node.prev = null;
			
			this.nodeMap.remove(value);
		}
	}
	
	public T getNext() {
		if (this.currentNode != null) {
			this.currentNode = this.currentNode.next;
		}
		return (this.currentNode != null) ? this.currentNode.value : null;
	}
	
	public void rewind() {
		this.currentNode = (this.headNode != null) ? this.headNode.next : null;
	}
	
	public Collection<T> getAll() {
		return this.nodeMap.keySet();
	}
}
