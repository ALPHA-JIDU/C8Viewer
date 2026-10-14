import { ThreadRow, Msg } from '../types';

export function getSampleChatData(): {
  threads: ThreadRow[];
  messages: Msg[];
  me: string;
} {
  const me = 'Alex Taylor';

  const threads: ThreadRow[] = [
    {
      id: 1,
      title: 'Family Reunion 2024',
      folder: 'familyreunion2024_104829',
      people: 4,
      count: 14,
      lastTs: Date.now() - 1000 * 60 * 25, // 25 mins ago
      preview: "Can't wait to see everyone this weekend! Don't forget sunscreen.",
      participants: ['Alex Taylor', 'Sarah Connor', 'Marcus Vance', 'Elena Gilbert'],
    },
    {
      id: 2,
      title: 'Sarah Connor',
      folder: 'sarahconnor_59281',
      people: 2,
      count: 18,
      lastTs: Date.now() - 1000 * 60 * 60 * 3, // 3 hours ago
      preview: 'I sent over the flight itinerary for next month.',
      participants: ['Alex Taylor', 'Sarah Connor'],
    },
    {
      id: 3,
      title: 'Dev Squad & Hackathon',
      folder: 'devsquadhackathon_39218',
      people: 3,
      count: 12,
      lastTs: Date.now() - 1000 * 60 * 60 * 26, // yesterday
      preview: 'Check out this new documentation link:',
      participants: ['Alex Taylor', 'David Chen', 'Elena Gilbert'],
    },
    {
      id: 4,
      title: 'Marcus Vance',
      folder: 'marcusvance_77192',
      people: 2,
      count: 9,
      lastTs: Date.now() - 1000 * 60 * 60 * 72, // 3 days ago
      preview: 'Sounds good man, let me know when you arrive.',
      participants: ['Alex Taylor', 'Marcus Vance'],
    },
  ];

  const now = Date.now();
  const dayMs = 86400000;

  const messages: Msg[] = [
    // Thread 1: Family Reunion
    {
      id: 101,
      threadId: 1,
      sender: 'Marcus Vance',
      ts: now - dayMs * 2 - 3600000 * 4,
      content: 'Hey everyone, what time are we meeting at the beach house?',
      media: [],
      link: null,
      reactions: '👍 Alex Taylor\n❤️ Sarah Connor',
      unsent: false,
    },
    {
      id: 102,
      threadId: 1,
      sender: 'Sarah Connor',
      ts: now - dayMs * 2 - 3600000 * 3,
      content: 'Check-in is at 3:00 PM on Friday!',
      media: [],
      link: null,
      reactions: '❤️ Marcus Vance',
      unsent: false,
    },
    {
      id: 103,
      threadId: 1,
      sender: 'Elena Gilbert',
      ts: now - dayMs * 2 - 3600000 * 2,
      content: 'I brought the board games and barbecue supplies 🍖🍉',
      media: [],
      link: null,
      reactions: '🙌 Alex Taylor',
      unsent: false,
    },
    {
      id: 104,
      threadId: 1,
      sender: 'Alex Taylor',
      ts: now - dayMs - 3600000 * 5,
      content: "Awesome! I'll drive up with Marcus around noon. We can pick up groceries on the way.",
      media: [],
      link: null,
      reactions: '❤️ Sarah Connor',
      unsent: false,
    },
    {
      id: 105,
      threadId: 1,
      sender: 'Sarah Connor',
      ts: now - dayMs - 3600000 * 4,
      content: 'Here is a photo of the view from the deck!',
      media: [
        {
          type: 'photo',
          uri: 'your_facebook_activity/messages/inbox/familyreunion_104829/photos/deck_view.jpg',
          blobUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
        },
      ],
      link: null,
      reactions: '😍 Alex Taylor\n🔥 Marcus Vance',
      unsent: false,
    },
    {
      id: 106,
      threadId: 1,
      sender: 'Marcus Vance',
      ts: now - dayMs - 3600000 * 2,
      content: 'This was an accidental draft',
      media: [],
      link: null,
      reactions: null,
      unsent: true,
    },
    {
      id: 107,
      threadId: 1,
      sender: 'Elena Gilbert',
      ts: now - 1000 * 60 * 25,
      content: "Can't wait to see everyone this weekend! Don't forget sunscreen.",
      media: [],
      link: null,
      reactions: '❤️ Alex Taylor',
      unsent: false,
    },

    // Thread 2: Sarah Connor
    {
      id: 201,
      threadId: 2,
      sender: 'Sarah Connor',
      ts: now - dayMs * 5,
      content: 'Hey Alex, do you still have the concert tickets saved?',
      media: [],
      link: null,
      reactions: null,
      unsent: false,
    },
    {
      id: 202,
      threadId: 2,
      sender: 'Alex Taylor',
      ts: now - dayMs * 5 + 3600000,
      content: 'Yes! Downloaded the PDF receipt and saved barcodes.',
      media: [],
      link: null,
      reactions: '🎉 Sarah Connor',
      unsent: false,
    },
    {
      id: 203,
      threadId: 2,
      sender: 'Sarah Connor',
      ts: now - dayMs * 3,
      content: 'Check out this coffee shop nearby that opened yesterday:',
      media: [],
      link: 'https://maps.google.com/?q=Blue+Bottle+Coffee',
      reactions: '☕ Alex Taylor',
      unsent: false,
    },
    {
      id: 204,
      threadId: 2,
      sender: 'Alex Taylor',
      ts: now - dayMs * 3 + 1800000,
      content: 'Looks great! Let us grab matcha and lattes before heading over.',
      media: [
        {
          type: 'sticker',
          uri: 'your_facebook_activity/messages/stickers/thumbs_up.png',
          blobUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=400&q=80',
        },
      ],
      link: null,
      reactions: '❤️ Sarah Connor',
      unsent: false,
    },
    {
      id: 205,
      threadId: 2,
      sender: 'Sarah Connor',
      ts: now - 1000 * 60 * 60 * 3,
      content: 'I sent over the flight itinerary for next month.',
      media: [],
      link: null,
      reactions: null,
      unsent: false,
    },

    // Thread 3: Dev Squad
    {
      id: 301,
      threadId: 3,
      sender: 'David Chen',
      ts: now - dayMs * 2,
      content: 'Pushed the new migration PR. Please review when you have a moment!',
      media: [],
      link: 'https://github.com/ALPHA-JIDU/chatviewer',
      reactions: '🚀 Alex Taylor\n👍 Elena Gilbert',
      unsent: false,
    },
    {
      id: 302,
      threadId: 3,
      sender: 'Alex Taylor',
      ts: now - dayMs * 2 + 3600000,
      content: 'Reviewed and merged! The query optimization cut down search response times significantly.',
      media: [],
      link: null,
      reactions: '🙌 David Chen',
      unsent: false,
    },
    {
      id: 303,
      threadId: 3,
      sender: 'David Chen',
      ts: now - dayMs - 3600000 * 2,
      content: 'Check out this new documentation link:',
      media: [],
      link: 'https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API',
      reactions: null,
      unsent: false,
    },

    // Thread 4: Marcus Vance
    {
      id: 401,
      threadId: 4,
      sender: 'Marcus Vance',
      ts: now - dayMs * 4,
      content: 'Yo Alex, are you catching the game tonight?',
      media: [],
      link: null,
      reactions: null,
      unsent: false,
    },
    {
      id: 402,
      threadId: 4,
      sender: 'Alex Taylor',
      ts: now - dayMs * 4 + 7200000,
      content: "Yeah definitely, heading over to Jordan's place.",
      media: [],
      link: null,
      reactions: '🍻 Marcus Vance',
      unsent: false,
    },
    {
      id: 403,
      threadId: 4,
      sender: 'Marcus Vance',
      ts: now - dayMs * 3,
      content: 'Sounds good man, let me know when you arrive.',
      media: [],
      link: null,
      reactions: null,
      unsent: false,
    },
  ];

  return { threads, messages, me };
}
