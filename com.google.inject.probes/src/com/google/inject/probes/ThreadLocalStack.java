package com.google.inject.probes;

import java.util.Stack;

public class ThreadLocalStack<T> {
	private ThreadLocal<Stack<T>> local = new ThreadLocal<Stack<T>>();

	public void push(T t) {
		Stack<T> stack = local.get();
		if (stack == null)
			local.set(stack = new Stack<T>());
		stack.push(t);
	}

	public T peek() {
		Stack<T> stack = local.get();
		if (stack == null)
			return null;
		return stack.peek();
	}

	public T pop() {
		Stack<T> stack = local.get();
		if (stack == null || stack.isEmpty())
			return null;
		return stack.pop();
	}
}
