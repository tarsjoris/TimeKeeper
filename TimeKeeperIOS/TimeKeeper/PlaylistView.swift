//
//  ContentView.swift
//  TimeKeeper
//
//  Created by samsung on 25/05/2026.
//

import SwiftUI

struct PlaylistView: View {
    let id: UInt64?
    @StateObject private var service = SoundService()
    @State private var playlist: PlaylistData? = nil
    @State private var selectedId: UInt8? = nil
    var speechManager = SpeechManager()
    
    var body: some View {
            VStack {
                ScrollViewReader { proxy in
                    List(playlist?.songs ?? [], selection: $selectedId) { entry in
                        VStack() {
                            Text(entry.name)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedId = entry.id
                            guard let pl = playlist else { return }
                            if (pl.announceTitle) {
                                guard let selectedSong = pl.songs.first(where: { $0.id == entry.id}) else { return }
                                speechManager.speak(selectedSong.name)
                            }
                        }
                    }
                    .onChange(of: selectedId) { _, newSelection in
                        guard let newSelection else { return }
                        withAnimation {
                            proxy.scrollTo(newSelection, anchor: .center)
                        }
                    }
                }
            }
            .navigationTitle(playlist?.name ?? "<no name>")
            .onAppear(perform: loadPlaylist)
            .onDisappear(perform: stop)
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 8) {
                    button("Start") {
                        startSelectedSong()
                    }

                    button("Stop") {
                        stop()
                    }

                    button("Next") {
                        stop()
                        selectNextSong()
                    }
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .background(.bar)
            }
    }
    
    private func button(_ title: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(.blue))
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .foregroundColor(.white)
                    .padding()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .contentShape(Rectangle())
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
    }
    
    private func loadPlaylist() {
        selectedId = nil
        guard let safeId = id else { return }
        playlist = TimeKeeper.loadPlaylist(safeId)
        guard let firstSong = playlist?.songs.first else { return }
        selectedId = firstSong.id
    }
    
    private func startSelectedSong() {
        guard let id = selectedId else { return }
        guard let pl = playlist else { return }
        guard let selectedSong = pl.songs.first(where: { $0.id == id}) else { return }
        service.start(song: selectedSong, stereo: pl.stereo)
    }
    
    private func stop() {
        service.stop()
    }
    
    private func selectNextSong() {
        guard let id = selectedId else { return }
        guard let pl = playlist else { return }
        guard let index = pl.songs.firstIndex(where: { $0.id == id}) else { return }
        if (index + 1 < pl.songs.count) {
            selectedId = pl.songs[index + 1].id
        }
    }
}

#Preview {
    PlaylistView(id: nil)
}
