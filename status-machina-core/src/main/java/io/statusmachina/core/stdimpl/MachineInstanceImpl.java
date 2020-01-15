/*
 *  Copyright 2019 <---> Present Status Machina Contributors (https://github.com/entzik/status-machina/graphs/contributors)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package io.statusmachina.core.stdimpl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.statusmachina.core.api.*;
import io.statusmachina.core.spi.MachinePersistenceCallback;

import java.util.*;
import java.util.function.Consumer;

public class MachineInstanceImpl<S, E> implements Machine<S, E> {
    private final String id;

    private final MachineDefinition<S, E> def;
    private final ImmutableMap<String, String> context;
    private final ImmutableList<TransitionRecord<S, E>> history;
    private final Optional<String> error;

    private final S currentState;
    private final MachinePersistenceCallback<S,E> persistenceCallback;

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            MachinePersistenceCallback<S,E> persistenceCallback,
            Map<String, String> context
    ) throws Exception {
        this(def, UUID.randomUUID().toString(), persistenceCallback, context);
    }

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            String id,
            MachinePersistenceCallback<S,E> persistenceCallback,
            Map<String, String> context
    ) throws Exception {
        this(def, id, context, persistenceCallback);
    }

    public MachineInstanceImpl(
            MachineDefinition<S, E> def,
            String id,
            Map<String, String> context, MachinePersistenceCallback<S,E> persistenceCallback
    ) throws Exception {
        this.def = def;

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S, E>>builder().build();
        this.currentState = def.getInitialState();
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        this.error = Optional.empty();
        this.persistenceCallback = persistenceCallback;

        persistenceCallback.runInTransaction(() -> persistenceCallback.saveNew(this));
    }

    public MachineInstanceImpl(
            String id,
            MachineDefinition<S, E> def,
            S currentState,
            Map<String, String> context,
            List<TransitionRecord<S, E>> history,
            Optional<String> error,
            MachinePersistenceCallback<S,E> persistenceCallback
    ) throws TransitionException {
        this.def = def;

        this.id = id;
        this.history = ImmutableList.<TransitionRecord<S, E>>builder().addAll(history).build();
        this.error = error;
        this.currentState = currentState;
        this.context = ImmutableMap.<String, String>builder().putAll(context).build();
        this.persistenceCallback = persistenceCallback;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public S getCurrentState() {
        return currentState;
    }

    @Override
    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    @Override
    public List<TransitionRecord<S, E>> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public Optional<String> getError() {
        return error;
    }

    @Override
    public MachineDefinition<S, E> getDefinition() {
        return def;
    }

    @Override
    public boolean isErrorState() {
        return error.isPresent();
    }

    public Machine<S, E> start() {
        if (currentState.equals(def.getInitialState()))
            return tryStp();
        else
            throw new IllegalStateException("machine is already started");
    }


    @Override
    public Machine<S,E>  sendEvent(E event) throws TransitionException {
        Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        return applyTransition(transition, null);
    }

    @Override
    public <P> Machine<S,E> sendEvent(E event, P param) throws TransitionException {
        final Transition<S,E> transition = def.findEventTransion(currentState, event).orElseThrow(() -> new IllegalStateException("for machines of type " + def.getName() + " event " + event.toString() + " does not trigger any transition out of state " + currentState.toString()));
        return applyTransition(transition, param);
    }

/*
    @Override
    public Machine<S, E> sendEvent(E event) throws TransitionException {
        return sendEvent(event);
    }

    @Override
    public <P> Machine<S, E> sendEvent(E event, P param) throws TransitionException {
        return sendEvent(event, param);
    }
*/

    @Override
    public Machine<S, E> recoverFromError(S state, Map<String, String> context) {
        Optional<String> newError = Optional.empty();
        ImmutableMap<String, String> newContext = ImmutableMap.<String, String>builder().putAll(context).build();

        return new MachineInstanceImpl<S, E>(id, def, state, newContext, history, newError, persistenceCallback);
    }

    private Machine<S, E> tryStp() throws TransitionException {
        if (this.isErrorState())
            return this;
        else {
            final Optional<Transition<S, E>> stpTransition = def.findStpTransition(currentState);
            if (stpTransition.isPresent()) {
                return applyTransition(stpTransition.get(), null);
            } else
                return this;
        }
    }

    private <P> Machine<S, E> applyTransition(Transition<S, E> transition, P param) throws TransitionException {
        try {
            final Machine<S, E> machine = persistenceCallback.runInTransaction(() -> {
                final Optional<TransitionAction<?>> action = transition.getAction();
                try {
                    ImmutableMap<String, String> newContext = action.map(mapConsumer -> ((TransitionAction<P>) mapConsumer).apply(context, param)).orElse(context);
                    Optional<String> newError = Optional.empty();
                    S newState = transition.getTo();
                    final MachineInstanceImpl<S, E> newMachine = new MachineInstanceImpl<>(id, def, newState, newContext, history, newError, persistenceCallback);
                    return persistenceCallback.update(newMachine);
                } catch (Throwable t) {
                    def.getErrorHandler().accept(new DefaultErrorData<>(transition, param, t));
                    Optional<String> newError = Optional.of(t.getMessage());
                    final MachineInstanceImpl<S, E> newMachine = new MachineInstanceImpl<>(id, def, currentState, context, history, newError, persistenceCallback);
                    return persistenceCallback.update(newMachine);
                }
            });
            return ((MachineInstanceImpl) machine).tryStp();
        } catch (Exception e) {
            throw new TransitionException(this, transition, e);
        }
    }


    @Override
    public boolean isTerminalState() {
        return def.getTerminalStates().contains(currentState);
    }

    private static class VoidMachineConsumer<S, E> implements Consumer<Machine<S, E>> {
        @Override
        public void accept(Machine<S, E> seMachine) {
        }
    }

    private class StateAndContext {
        private final S state;
        private ImmutableMap<String, String> context;

        public StateAndContext(S state, ImmutableMap<String, String> context) {
            this.state = state;
            this.context = context;
        }

        public S getState() {
            return state;
        }

        public ImmutableMap<String, String> getContext() {
            return context;
        }
    }


    private class DefaultErrorData<S, E, P> implements ErrorData<S, E> {
        private final Transition<S, E> transition;
        private final P param;
        private final Throwable t;

        public DefaultErrorData(Transition<S, E> transition, P param, Throwable t) {
            this.transition = transition;
            this.param = param;
            this.t = t;
        }

        @Override
        public S getFrom() {
            return transition.getFrom();
        }

        @Override
        public S getTo() {
            return transition.getTo();
        }

        @Override
        public Optional<E> getEvent() {
            return transition.getEvent();
        }

        @Override
        public Map<String, String> getContext() {
            return new HashMap<>(context);
        }

        @Override
        public P getEventParameter() {
            return param;
        }

        @Override
        public String getErrorMessage() {
            return t.getMessage();
        }
    }
}