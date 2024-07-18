package com.example.application.views.channel;

import com.example.application.chat.ChatService;
import com.example.application.chat.Message;
import com.example.application.views.MainLayout;
import com.example.application.views.lobby.LobbyView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Kommentar für ein test
 * Das ist ein weiterer Kommentar
 */
@Route(value = "channel", layout = MainLayout.class)
public class ChannelView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {

    @Autowired
    private ChatService chatService;
    private MessageList messageList;
    private String channelId;
    private String channelName;
    private final List<Message> receivedMessages = new ArrayList<>();

    public ChannelView(ChatService chatService) {
        this.chatService = chatService;
        setSizeFull();

        this.messageList = new MessageList();
        this.messageList.setSizeFull();
        add(messageList);

        var messageInput = new MessageInput(submitEvent -> sendMessage(submitEvent.getValue()));
        messageInput.setWidthFull();
        add(messageInput);
    }

    @Override
    public void setParameter(BeforeEvent event, String channelId) {
        if(chatService.channel(channelId).isEmpty()) {
            event.forwardTo(LobbyView.class);
        }
        this.channelName = chatService.channel(channelId).get().name();
        this.channelId = channelId;
    }

    @Override
    public String getPageTitle() {
        return channelName;
    }

    private void sendMessage(String message) {
        if (!message.isBlank()) {
            chatService.postMessage(channelId, message);
        }
    }

    private MessageListItem createMessageListItem(Message message) {
        MessageListItem item = new MessageListItem(
                message.message(),
                message.timestamp(),
                message.author()
        );

        return item;
    }

    private void receiveMessage(List<Message> incomingMessages) {
        getUI().ifPresent(ui -> ui.access(() -> {
            receivedMessages.addAll(incomingMessages);
            messageList.setItems(receivedMessages
                    .stream()
                    // syntax sugar (method reference) the same as map(message -> this.createMessageListItem(message)
                    .map(this::createMessageListItem)
                    .toList());
        }));
    }

    private Disposable subscribe() {
        return chatService.liveMessages(channelId)
                .subscribe(this::receiveMessage);
    }

    protected void onAttach(AttachEvent attachEvent){
        var subscription = subscribe();
        addDetachListener(detachEvent -> subscription.dispose());
    }
}
